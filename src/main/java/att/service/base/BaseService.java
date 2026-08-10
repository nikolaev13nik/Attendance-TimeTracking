package att.service.base;

import att.context.DataTimeContext;

public interface BaseService<T> {
    void execute(DataTimeContext<T> context);

    // use this when you want to run the pipeline with your own transaction boundary
    default void executeWithoutTransactional(DataTimeContext<T> context) {
    }
}