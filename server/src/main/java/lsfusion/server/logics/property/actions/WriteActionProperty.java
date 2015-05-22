package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WriteActionProperty extends ScriptingActionProperty {
    private final LCP<?> sourceProp;
    
    public WriteActionProperty(ScriptingLogicsModule LM, ValueClass valueClass, LCP<?> sourceProp) {
        super(LM, valueClass);
        this.sourceProp = sourceProp;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject value = context.getDataKeys().getValue(0);
        assert value.getType() instanceof StringClass;

        String path = (String) value.object;
        byte[] fileBytes = (byte[]) sourceProp.read(context);
        if(sourceProp.property.getType() instanceof DynamicFormatFileClass) {
            fileBytes = BaseUtils.getFile(fileBytes);
        }
        try {
            if (path != null && fileBytes != null) {
                Pattern p = Pattern.compile("(file|ftp):\\/\\/(.*)");
                Matcher m = p.matcher(path);
                if (m.matches()) {
                    String type = m.group(1).toLowerCase();
                    String url = m.group(2);
                    
                    if (type.equals("file")) {
                        File file = new File(url);
                        if(!file.getParentFile().exists() && !file.getParentFile().mkdirs())
                            throw Throwables.propagate(new RuntimeException(String.format("Path is incorrect or not found: %s", url)));
                        else
                            IOUtils.putFileBytes(file, fileBytes);
                    } else if (type.equals("ftp")) {
                        File file = File.createTempFile("downloaded", ".tmp");
                        IOUtils.putFileBytes(file, fileBytes);
                        storeFileToFTP(path, file);
                        file.delete();
                    }
                } else {
                    throw Throwables.propagate(new RuntimeException("Incorrect path. Please use format: file://path_to_file or ftp://username:password@host:port/path_to_file"));
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void storeFileToFTP(String path, File file) throws IOException {
        /*ftp://username:password@host:port/path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("ftp:\\/\\/(.*):(.*)@([^\\/:]*)(?::([^\\/]*))?(?:\\/(.*))?");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String username = connectionStringMatcher.group(1); //lstradeby
            String password = connectionStringMatcher.group(2); //12345
            String server = connectionStringMatcher.group(3); //ftp.harmony.neolocation.net
            boolean noPort = connectionStringMatcher.group(4) == null;
            Integer port = noPort ? 21 : Integer.parseInt(connectionStringMatcher.group(4)); //21
            String remoteFile = connectionStringMatcher.group(5);
            FTPClient ftpClient = new FTPClient();
            try {

                ftpClient.connect(server, port);
                ftpClient.login(username, password);
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                
                InputStream inputStream = new FileInputStream(file);
                boolean done = ftpClient.storeFile(remoteFile, inputStream);
                inputStream.close();
                if (!done) {
                    throw Throwables.propagate(new RuntimeException("Some error occurred while downloading file from ftp"));
                }
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            throw Throwables.propagate(new RuntimeException("Incorrect ftp url. Please use format: ftp://username:password@host:port/path_to_file"));
        }
    }
}
