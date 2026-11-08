package att.context;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import att.client.accounting.dto.UserProfileDto;
import att.dto.MonthlyUserStatisticInfoDto;
import att.model.DataTime;
import att.model.LeaveDay;
import att.model.MonthStatistic;
import lombok.Data;

@Data
public class StatisticInfoHolder {
    Table<Integer, OffsetDateTime, MonthlyUserStatisticInfoDto> multipleUserData = HashBasedTable.create();
    Collection<Integer> targetUserIds = new ArrayList<>();
    Map<Integer, UserProfileDto> targetUserProfiles = new HashMap<>();
    List<MonthStatistic> monthStatisticList = new ArrayList<>();
    List<LeaveDay> fullLeaveDaysWithWork = new ArrayList<>();
    List<DataTime> overlappingSessions = new ArrayList<>();
    private OffsetDateTime startOfMonth;
    private Boolean report;
    private UserProfileDto reporterProfile;

}
