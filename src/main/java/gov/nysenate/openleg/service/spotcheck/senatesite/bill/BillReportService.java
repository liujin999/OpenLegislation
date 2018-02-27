package gov.nysenate.openleg.service.spotcheck.senatesite.bill;

import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.bill.reference.senatesite.SenateSiteDao;
import gov.nysenate.openleg.dao.spotcheck.BillIdSpotCheckReportDao;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckReportDao;
import gov.nysenate.openleg.model.base.PublishStatus;
import gov.nysenate.openleg.model.bill.BaseBillId;
import gov.nysenate.openleg.model.bill.Bill;
import gov.nysenate.openleg.model.bill.BillId;
import gov.nysenate.openleg.model.spotcheck.*;
import gov.nysenate.openleg.model.spotcheck.senatesite.SenateSiteDump;
import gov.nysenate.openleg.model.spotcheck.senatesite.SenateSiteDumpFragment;
import gov.nysenate.openleg.model.spotcheck.senatesite.SenateSiteDumpId;
import gov.nysenate.openleg.model.spotcheck.senatesite.bill.SenateSiteBill;
import gov.nysenate.openleg.service.bill.data.BillDataService;
import gov.nysenate.openleg.service.bill.data.BillNotFoundEx;
import gov.nysenate.openleg.service.spotcheck.base.BaseSpotCheckReportService;
import gov.nysenate.openleg.util.AsyncUtils;
import gov.nysenate.openleg.util.pipeline.Pipeline;
import gov.nysenate.openleg.util.pipeline.PipelineFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Service
public class BillReportService extends BaseSpotCheckReportService<BillId> {

    private static final Logger logger = LoggerFactory.getLogger(BillReportService.class);

    @Autowired private AsyncUtils asyncUtils;
    @Autowired private PipelineFactory pipelineFactory;

    @Autowired private BillIdSpotCheckReportDao billReportDao;
    @Autowired private SenateSiteDao senateSiteDao;
    @Autowired private BillJsonParser billJsonParser;

    @Autowired private BillDataService billDataService;

    @Autowired private BillCheckService billCheckService;

    @Value("${spotcheck.website.bill.ref_queue_size:500}")
    private int refQueueSize;

    @Value("${spotcheck.website.bill.data_queue_size:500}")
    private int dataQueueSize;

    @Override
    protected SpotCheckReportDao<BillId> getReportDao() {
        return billReportDao;
    }

    @Override
    public SpotCheckRefType getSpotcheckRefType() {
        return SpotCheckRefType.SENATE_SITE_BILLS;
    }

    @Override
    public synchronized SpotCheckReport<BillId> generateReport(LocalDateTime start, LocalDateTime end) throws Exception {
        SenateSiteDump billDump = getMostRecentDump();
        SpotCheckReportId reportId = new SpotCheckReportId(SpotCheckRefType.SENATE_SITE_BILLS,
                billDump.getDumpId().getDumpTime(), LocalDateTime.now());
        SpotCheckReport<BillId> report = new SpotCheckReport<>(reportId);
        report.setNotes(billDump.getDumpId().getNotes());
        try {
            generateReport(billDump, report);
        } finally {
            logger.info("archiving bill dump...");
            senateSiteDao.setProcessed(billDump);
        }
        return report;
    }

    /* --- Internal Methods --- */

    /**
     * Populate report with observations given senate site dump
     */
    private void generateReport(SenateSiteDump billDump, SpotCheckReport<BillId> report) {

        // Set up a pipeline: dump parsing -> bill retrieval -> checking

        BillChecker billChecker = new BillChecker(getBillIdsForSession(billDump));

        Pipeline<SenateSiteDumpFragment, SpotCheckObservation<BillId>> pipeline =
                pipelineFactory.<SenateSiteDumpFragment>pipelineBuilder()
                        .addTask(billJsonParser::extractBillsFromFragment, refQueueSize)
                        .addTask(this::getBill, dataQueueSize, 2)
                        .addTask(billChecker)
                        .build();

        pipeline.addInput(billDump.getDumpFragments());
        // Wait for pipeline to finish and add observations to report
        report.addObservations(pipeline.run().join());

        // Record ref missing mismatches from unchecked openleg bills
        generateRefMissingObs(billChecker.getUncheckedBaseBillIds(), billChecker.getUncheckedBillIds(), report);
    }

    /**
     * Gets the {@link Bill} corresponding to the given {@link SenateSiteBill} and packages them in a Pair.
     * Substitutes an empty optional for the bill of it does not exist in openleg
     *
     * @param refBill {@link SenateSiteBill}
     * @return Collection<Pair<SenateSiteBill, Optional<Bill>>>
     */
    private Collection<Pair<SenateSiteBill, Optional<Bill>>> getBill(SenateSiteBill refBill) {
        BillId billId = refBill.getBillId();
        Optional<Bill> olBill;
        try {
            olBill = Optional.of(
                    billDataService.getBill(BaseBillId.of(billId)));
        } catch (BillNotFoundEx ex) {
            olBill = Optional.empty();
        }
        return Collections.singletonList(Pair.of(refBill, olBill));
    }

    /**
     * An object that performs checks on {@link SenateSiteBill} against {@link Bill}s,
     * while keeping track of {@link Bill}s that had no {@link SenateSiteBill} counterpart.
     */
    private class BillChecker
            implements Function<Pair<SenateSiteBill, Optional<Bill>>, Collection<SpotCheckObservation<BillId>>> {

        /*
         *  Keep track of which openleg amendments were checked.
         *
         *  If no amendments of a bill were checked, it will be in the unchecked base bill id set.
         *  When a check is performed on a bill for the first time, all of its amendments
         *  will be added to the unchecked bill id set and it will be removed from the base bill id set.
         *  Each time an amendment is checked, it will be removed from the bill id set.
         */
        private Set<BaseBillId> uncheckedBaseBillIds;
        private Set<BillId> uncheckedBillIds = new HashSet<>();

        public BillChecker( Set<BaseBillId> uncheckedBaseBillIds) {
            this.uncheckedBaseBillIds = new HashSet<>(uncheckedBaseBillIds);
        }

        @Override
        public Collection<SpotCheckObservation<BillId>> apply(Pair<SenateSiteBill, Optional<Bill>> checkData) {
            Optional<Bill> olBillOpt = checkData.getRight();
            SenateSiteBill refBill = checkData.getLeft();
            BillId billId = refBill.getBillId();
            BaseBillId baseBillId = BaseBillId.of(billId);

            SpotCheckObservation<BillId> observation;
            if (olBillOpt.isPresent()) {
                Bill olBill = olBillOpt.get();
                observation = billCheckService.check(olBill, refBill);
                if (uncheckedBaseBillIds.remove(baseBillId)) {
                    uncheckedBillIds.addAll(olBill.getAmendmentIds());
                }
            } else {
                observation = SpotCheckObservation.getObserveDataMissingObs(
                        refBill.getReferenceId(), billId);
            }
            uncheckedBillIds.remove(billId);
            return Collections.singletonList(observation);
        }


        public Set<BillId> getUncheckedBillIds() {
            return uncheckedBillIds;
        }

        public Set<BaseBillId> getUncheckedBaseBillIds() {
            return uncheckedBaseBillIds;
        }
    }

    private SenateSiteDump getMostRecentDump() throws IOException, ReferenceDataNotFoundEx {
        return senateSiteDao.getPendingDumps(SpotCheckRefType.SENATE_SITE_BILLS).stream()
                .filter(SenateSiteDump::isComplete)
                .max(SenateSiteDump::compareTo)
                .orElseThrow(() -> new ReferenceDataNotFoundEx("Found no full senate site bill dumps"));
    }

    /**
     * Gets a set of all openleg bill ids for the session of the given dump
     *
     * @param billDump SenateSiteBillDump
     * @return Set<Bill>
     */
    private Set<BaseBillId> getBillIdsForSession(SenateSiteDump billDump) {
        SenateSiteDumpId dumpId = billDump.getDumpId();
        return new TreeSet<>(
                billDataService.getBillIds(dumpId.getSession(), LimitOffset.ALL)
        );
    }

    /**
     * Generate reference data missing observations for all openleg bills that were not present in the dump.
     *
     * @param uncheckedBaseBillIds {@link Set<BaseBillId>} - ids for bills with 0 checked amendments
     * @param uncheckedBillIds {@link Set<BillId>} - ids for bill amendments that were never checked
     * @param report SpotCheckReport - ref missing obs. will be added to this report
     */
    private void generateRefMissingObs(Set<BaseBillId> uncheckedBaseBillIds,
                                       Set<BillId> uncheckedBillIds,
                                       SpotCheckReport<BillId> report) {
        for (BaseBillId baseBillId : uncheckedBaseBillIds) {
            Bill bill = billDataService.getBill(baseBillId);
            uncheckedBillIds.addAll(bill.getAmendmentIds());
        }

        for (BillId billId : uncheckedBillIds) {
            Bill bill = billDataService.getBill(BaseBillId.of(billId));
            boolean published = bill.getPublishStatus(billId.getVersion())
                    .map(PublishStatus::isPublished)
                    .orElse(false);
            if (published) {
                report.addRefMissingObs(billId);
            } else {
                // Add empty observation for unpublished bills not in dump
                // Because unpublished bills shouldn't be on NYSenate.gov
                report.addEmptyObservation(billId);
            }
        }
    }

}
