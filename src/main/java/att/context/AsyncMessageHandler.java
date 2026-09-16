package att.context;

public interface AsyncMessageHandler {

    void prepareAndSendAsyncStatMsg(DataTimeContext<?> context);

}
