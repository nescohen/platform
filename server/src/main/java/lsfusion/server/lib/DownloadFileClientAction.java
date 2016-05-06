package lsfusion.server.lib;

import lsfusion.base.SystemUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class DownloadFileClientAction implements ClientAction {
    String path;
    String filename;
    public byte[] bytes;

    public DownloadFileClientAction(String path, String filename, byte[] bytes) {
        this.path = path;
        this.filename = filename;
        this.bytes = bytes;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            File dllFile = new File(System.getProperty("user.home", "") + "/.fusion/" + filename);
            FileUtils.writeByteArrayToFile(dllFile, bytes);
        } catch (Exception ignored) {

        }
        return null;
    }
}