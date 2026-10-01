package att.context;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import att.client.accounting.dto.UserProfileDto;
import att.dto.MonthlyUserStatisticInfoDto;
import att.model.MonthStatistic;
import lombok.Data;

@Data
public class StatisticInfoHolder {
    // api statistic can have only one the same required month but wit will usefull for future dev for report last six
    // month stat per one user.
    // For response
    Table<Integer, OffsetDateTime, MonthlyUserStatisticInfoDto> multipleUserData = HashBasedTable.create();
    Collection<Integer> targetUserIds = new ArrayList<>();
    //for save
    List<MonthStatistic> monthStatisticList = new ArrayList<>();
    private OffsetDateTime startOfMonth;
    private Boolean report;
    // the caller (JWT sub) the report was requested by, resolved from Attendance-Accounting only when report=true;
    // its name/email travel on every statistic event so the downstream AI service knows who to notify
    private UserProfileDto reportRequester;

}
