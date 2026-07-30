package att.service.base;

import att.context.DataTimeContext;

public interface BaseService<R> {
    void execute(DataTimeContext<R> context);

    // use this when you want to run the pipeline with your own transaction boundary
    default void executeWithoutTransactional(DataTimeContext<R> context) {
    }
}