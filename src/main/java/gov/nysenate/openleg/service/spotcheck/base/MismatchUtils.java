package gov.nysenate.openleg.service.spotcheck.base;

import gov.nysenate.openleg.model.spotcheck.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MismatchUtils {


    public static List<DeNormSpotCheckMismatch> copyIgnoreStatuses(List<DeNormSpotCheckMismatch> from, List<DeNormSpotCheckMismatch> to) {
        for (DeNormSpotCheckMismatch mismatch : to) {
            if (from.contains(mismatch)) {
                mismatch.setIgnoreStatus(from.get(from.indexOf(mismatch)).getIgnoreStatus());
            }
        }
        return to;
    }

    /**
     * Updates the ignore status of the given references.
     * @param reportMismatches
     * @return List of DeNormSpotCheckMismatch's with ignore statuses updated.
     */
    public static List<DeNormSpotCheckMismatch> updateIgnoreStatus(List<DeNormSpotCheckMismatch> reportMismatches) {
        return reportMismatches.stream()
                .map(MismatchUtils::calculateIgnoreStatus)
                .collect(Collectors.toList());
    }

    private static DeNormSpotCheckMismatch calculateIgnoreStatus(DeNormSpotCheckMismatch mismatch) {
        if (mismatch.getState() == MismatchState.CLOSED) {
            mismatch.setIgnoreStatus(ignoreStatusForClosed(mismatch));
        } else {
            mismatch.setIgnoreStatus(ignoreStatusForOpen(mismatch));
        }
        return mismatch;
    }

    private static SpotCheckMismatchIgnore ignoreStatusForClosed(DeNormSpotCheckMismatch mismatch) {
        if (mismatch.getIgnoreStatus() == SpotCheckMismatchIgnore.IGNORE_PERMANENTLY) {
            return SpotCheckMismatchIgnore.IGNORE_PERMANENTLY;
        }
        return SpotCheckMismatchIgnore.NOT_IGNORED;
    }

    private static SpotCheckMismatchIgnore ignoreStatusForOpen(DeNormSpotCheckMismatch mismatch) {
        if (mismatch.getIgnoreStatus() == SpotCheckMismatchIgnore.IGNORE_ONCE) {
            return SpotCheckMismatchIgnore.NOT_IGNORED;
        }
        return mismatch.getIgnoreStatus();
    }

    /**
     * Returns a list of mismatches that have been closed by a spotcheck report.
     * Mismatches are closed if they were checked by the report (in checkedKeys and checkedTypes),
     * are not in the report mismatches and are not already resolved.
     *
     * @param reportMismatches  New mismatches generated by a report.
     * @param currentMismatches All the most recent mismatches for the datasource checked by the report.
     * @param checkedKeys       All contentKey's checked by the report.
     * @param checkedTypes      All SpotCheckMismatchType's checked by the report.
     * @param reportDateTime    The report date time to set for any resolved mismatches.
     * @param referenceDateTime The reference date time to set for any resolved mismatches.
     * @return A list of mismatches resolved by this report.
     */
    public static List<DeNormSpotCheckMismatch> deriveClosedMismatches(List<DeNormSpotCheckMismatch> reportMismatches,
                                                                       List<DeNormSpotCheckMismatch> currentMismatches,
                                                                       Set<Object> checkedKeys,
                                                                       Set<SpotCheckMismatchType> checkedTypes,
                                                                       LocalDateTime reportDateTime,
                                                                       LocalDateTime referenceDateTime) {
        return currentMismatches.stream()
                .filter(m -> m.getState() != MismatchState.CLOSED)
                .filter(m -> checkedKeys.contains(m.getKey()))
                .filter(m -> checkedTypes.contains(m.getType()))
                .filter(m -> !reportMismatches.contains(m))
                // Update mismatch, setting to closed and updating dates.
                .peek(m -> m.setState(MismatchState.CLOSED))
                .peek(m -> m.setReportDateTime(reportDateTime))
                .peek(m -> m.setReferenceDateTime(referenceDateTime))
                .collect(Collectors.toList());
    }


}