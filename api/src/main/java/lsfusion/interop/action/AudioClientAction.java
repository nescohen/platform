package lsfusion.interop.action;

import lsfusion.base.file.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class AudioClientAction extends ExecuteClientAction {

    public byte[] audio;

    public AudioClientAction(InputStream in) throws IOException {
        this(IOUtils.readBytesFromStream(in));
    }

    public AudioClientAction(byte[] audio) {
        this.audio = audio;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
