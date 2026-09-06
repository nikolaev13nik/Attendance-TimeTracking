package att.context;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;


import att.dto.MonthlyUserStatisticInfoDto;
import lombok.Data;

@Data
public class StatisticInfoHolder {

    Table<Integer, OffsetDateTime, MonthlyUserStatisticInfoDto> multipleUserData = HashBasedTable.create();
    Map<Integer, Integer> vacationPerUser = new HashMap<>();
    Map<Integer, Integer> seekDayPerUser = new HashMap<>();
    Map<Integer, Long> totalWorkHours = new HashMap<>();
    Map<Integer, Integer> totalWorkDays = new HashMap<>();
    private OffsetDateTime startOfMonth;
    private Boolean report;

}
