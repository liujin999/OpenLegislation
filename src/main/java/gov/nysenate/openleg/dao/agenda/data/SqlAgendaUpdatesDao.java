package gov.nysenate.openleg.dao.agenda.data;

import com.google.common.collect.Range;
import gov.nysenate.openleg.dao.base.*;
import gov.nysenate.openleg.model.agenda.AgendaId;
import gov.nysenate.openleg.model.calendar.CalendarId;
import gov.nysenate.openleg.model.updates.UpdateDigest;
import gov.nysenate.openleg.model.updates.UpdateToken;
import gov.nysenate.openleg.model.updates.UpdateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

import static gov.nysenate.openleg.dao.agenda.data.SqlAgendaUpdatesQuery.*;

@Repository
public class SqlAgendaUpdatesDao extends SqlBaseDao implements AgendaUpdatesDao
{
    private static final Logger logger = LoggerFactory.getLogger(SqlAgendaUpdatesDao.class);

    /** {@inheritDoc} */
    @Override
    public PaginatedList<UpdateToken<AgendaId>> getUpdates(Range<LocalDateTime> dateTimeRange, UpdateType type,
                                                           SortOrder dateOrder, LimitOffset limOff) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        addDateTimeRangeParams(params, dateTimeRange);

        String sqlQuery = getSqlQuery(false, false, type, dateOrder, limOff);
        PaginatedRowHandler<UpdateToken<AgendaId>> handler =
            new PaginatedRowHandler<>(limOff, "total_updated", agendaUpdateTokenRowMapper);
        jdbcNamed.query(sqlQuery, params, handler);
        return handler.getList();
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<UpdateDigest<AgendaId>> getDetailedUpdates(Range<LocalDateTime> dateTimeRange, UpdateType type,
                                                                    SortOrder dateOrder, LimitOffset limOff) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        addDateTimeRangeParams(params, dateTimeRange);

        String sqlQuery = getSqlQuery(true, false, type, dateOrder, limOff);
        PaginatedRowHandler<UpdateDigest<AgendaId>> handler =
            new PaginatedRowHandler<>(limOff, "total_updated", agendaUpdateDigestRowMapper);
        jdbcNamed.query(sqlQuery, params, handler);
        return handler.getList();
    }

    /** {@inheritDoc} */
    @Override
    public PaginatedList<UpdateDigest<AgendaId>> getDetailedUpdatesForAgenda(
        AgendaId agendaId, Range<LocalDateTime> dateTimeRange, UpdateType type, SortOrder dateOrder, LimitOffset limOff) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("agendaNo", agendaId.getNumber()).addValue("year", agendaId.getYear());
        addDateTimeRangeParams(params, dateTimeRange);

        String sqlQuery = getSqlQuery(true, true, type, dateOrder, limOff);
        PaginatedRowHandler<UpdateDigest<AgendaId>> handler =
            new PaginatedRowHandler<>(limOff, "total_updated", agendaUpdateDigestRowMapper);
        jdbcNamed.query(sqlQuery, params, handler);
        return handler.getList();
    }

    /** --- Internal --- */

    private static final RowMapper<UpdateToken<AgendaId>> agendaUpdateTokenRowMapper = (rs, rowNum) ->
        new UpdateToken<>(
            new AgendaId(rs.getInt("agenda_no"), rs.getInt("year")),
            rs.getString("last_fragment_id"), getLocalDateTimeFromRs(rs, "last_published_date_time"),
            getLocalDateTimeFromRs(rs, "last_processed_date_time"));

    private static RowMapper<UpdateDigest<AgendaId>> agendaUpdateDigestRowMapper = (rs, rowNum) -> {
        Map<String, String> key = getHstoreMap(rs, "key");
        AgendaId id = new AgendaId(Integer.parseInt(key.remove("agenda_no")), Integer.parseInt(key.remove("year")));
        UpdateDigest<AgendaId> digest = new UpdateDigest<>(agendaUpdateTokenRowMapper.mapRow(rs, rowNum));
        Map<String, String> data = getHstoreMap(rs, "data");
        data.putAll(key);

        digest.setFields(data);
        digest.setAction(rs.getString("action"));
        digest.setTable(rs.getString("table_name"));
        return digest;
    };

    /**
     * Generates the appropriate sql query based on the args, to remove code duplication.
     */
    private String getSqlQuery(boolean detail, boolean specificAgenda, UpdateType updateType, SortOrder sortOrder,
                               LimitOffset limOff) {
        String dateColumn;
        // The UpdateType dictates which date columns we used to search by
        OrderBy orderBy;
        if (updateType.equals(UpdateType.PROCESSED_DATE)) {
            dateColumn = "log.action_date_time";
            orderBy = new OrderBy("last_processed_date_time", sortOrder);
        }
        else if (updateType.equals(UpdateType.PUBLISHED_DATE)) {
            dateColumn = "sobi.published_date_time";
            orderBy = new OrderBy("last_published_date_time", sortOrder, "last_processed_date_time", sortOrder);
        }
        else {
            throw new IllegalArgumentException("Cannot provide agenda updates of type: " + updateType);
        }
        String sqlQuery;
        if (specificAgenda) {
            sqlQuery = SELECT_UPDATE_DIGESTS_FOR_SPECIFIC_AGENDA.getSql(schema(), orderBy, limOff);
        }
        else {
            sqlQuery = (detail) ? SELECT_AGENDA_UPDATE_DIGESTS.getSql(schema(), orderBy, limOff)
                                : SELECT_AGENDA_UPDATE_TOKENS.getSql(schema(), orderBy, limOff);
        }
        sqlQuery = queryReplace(sqlQuery, "dateColumn", dateColumn);
        return sqlQuery;
    }
}
