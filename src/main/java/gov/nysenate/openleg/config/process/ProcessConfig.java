package gov.nysenate.openleg.config.process;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import gov.nysenate.openleg.model.sourcefiles.SourceType;
import gov.nysenate.openleg.model.sourcefiles.sobi.SobiBlock;
import gov.nysenate.openleg.model.sourcefiles.sobi.SobiFragment;
import gov.nysenate.openleg.processor.base.AbstractDataProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProcessConfig extends AbstractDataProcessor {

    private RangeMap<Integer, SourceFilter> filterRangeMap;
    private SourceFilter xmlFilter;
    private SourceFilter sobiFilter;

    //default constructor
    public ProcessConfig() {}

    @PostConstruct
    public void init() {
        xmlFilter = new XmlWhitelistFilter();
        sobiFilter = new SobiWhitelistFilter();

        filterRangeMap = ImmutableRangeMap.<Integer, SourceFilter>builder()
                .put(Range.lessThan(2009), xmlFilter)
                .put(Range.closedOpen(2009, 2018), sobiFilter)
                .put(Range.atLeast(2018), xmlFilter)
                .build();
    }

    /**
     * This method filters the given list of fragments,
     * returning a list that is filtered according to the process config.
     *
     * @param fragments {@link SobiFragment}
     * @return {@link List<SobiFragment>}
     */
    public List<SobiFragment> filterFileFragments(List<SobiFragment> fragments) {
        return fragments.stream()
                .filter(frag -> determineProcessWhitelist(frag.getPublishedDateTime(),
                            frag.getParentSobiFile().getSourceType())
                            .acceptFragment(frag)
                )
                .collect(Collectors.toList());
    }

    /**
     * This method filters the given list of sobi blocks,
     * returning a list that is filtered according to the process config.
     *
     * @param blocks {@link List<SobiBlock>}
     * @return {@link List<SobiBlock>}
     */
    public List<SobiBlock> filterSobiBlocks( List<SobiBlock> blocks) {
        return blocks.stream()
                .filter(block -> sobiFilter.acceptBlock(block))
                .collect(Collectors.toList());
    }

    private SourceFilter determineProcessWhitelist(LocalDateTime publishDate,SourceType sourceType) {
        return determineProcessWhitelist(publishDate.toLocalDate(), sourceType);
    }

    public SourceFilter determineProcessWhitelist(LocalDate publishDate,SourceType sourceType) {
        if (sourceType == SourceType.XML || sourceType == SourceType.SOBI) {
            return filterRangeMap.get(publishDate.getYear());
        }
        else {
            throw new IllegalStateException("Unknown source type: " + sourceType);
        }
    }
}