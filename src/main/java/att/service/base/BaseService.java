package att.service.base;

import att.context.DataTimeContext;

public interface BaseService<T> {
    void execute(DataTimeContext<T> context);

    default void executeWithoutTransactional(DataTimeContext<T> context) {
    }
}