package gov.nysenate.openleg.config.process;

import gov.nysenate.openleg.model.sourcefiles.SourceType;
import gov.nysenate.openleg.model.sourcefiles.LegDataFragmentType;
import gov.nysenate.openleg.model.sourcefiles.sobi.SobiBlock;
import gov.nysenate.openleg.model.sourcefiles.LegDataFragment;
import gov.nysenate.openleg.model.sourcefiles.sobi.SobiLineType;

import static gov.nysenate.openleg.model.sourcefiles.SourceType.*;
import static gov.nysenate.openleg.model.sourcefiles.LegDataFragmentType.*;

/**
 * Accepts only xml data with the exception of floor votes.
 */
public class XmlNoFlvotWhitelistFilter implements SourceFilter {

    @Override
    public boolean acceptFragment(LegDataFragment legDataFragment) {
        SourceType sourceType = legDataFragment.getParentLegDataFile().getSourceType();
        LegDataFragmentType fragmentType = legDataFragment.getType();
        // Allow either non-floor-vote xmls or bill sobis (which contain floor votes)
        return sourceType == XML && fragmentType != SENFLVOTE
                || sourceType == SOBI && fragmentType == BILL;
    }

    @Override
    public boolean acceptBlock(SobiBlock sobiBlock) {
        // Only allow floor vote sobi blocks
        return sobiBlock.getType() == SobiLineType.VOTE_MEMO;
    }
}
