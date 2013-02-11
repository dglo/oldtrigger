package icecube.daq.oldtrigger.test;

import icecube.daq.payload.IWriteablePayload;

import java.nio.ByteBuffer;

public interface PayloadValidator
{
    void validate(ByteBuffer payBuf);
    void validate(IWriteablePayload payload);
}
