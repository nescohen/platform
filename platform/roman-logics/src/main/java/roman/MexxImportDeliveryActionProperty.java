package roman;

import platform.base.BaseUtils;
import platform.interop.action.MessageClientAction;
import platform.server.logics.DataObject;
import platform.server.logics.property.ExecutionContext;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MexxImportDeliveryActionProperty extends BaseImportActionProperty {

    public MexxImportDeliveryActionProperty(RomanLogicsModule LM) {
        super(LM, "Импортировать инвойс", LM.mexxSupplier, "zip");
    }


    @Override
    public void execute(ExecutionContext context) throws SQLException {
        try {

            DataObject supplier = context.getSingleKeyValue();

            List<byte[]> fileList = valueClass.getFiles(context.getValueObject());
            for (byte[] file : fileList) {

                ByteArrayInputStream stream = new ByteArrayInputStream(file);

                ZipInputStream zin = new ZipInputStream(stream);
                ZipEntry entry = zin.getNextEntry();
                byte[][] outputListInOrder = new byte[4][];

                while ((entry = zin.getNextEntry()) != null) {
                    String name = entry.getName();
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    int data = 0;
                    while ((data = zin.read()) != -1) {
                        output.write(data);
                    }

                    byte[] outputList = BaseUtils.bytesToBytes(output.toByteArray());
                    switch (name.charAt(0)) {
                        case 'W':
                            outputListInOrder[0] = outputList;
                            break;
                        case 'I':
                            outputListInOrder[1] = outputList;
                            break;
                        case 'G':
                            outputListInOrder[2] = outputList;
                            break;
                        case 'K':
                            outputListInOrder[3] = outputList;
                            break;
                    }

                }

                LM.mexxImportInvoice.execute(outputListInOrder[0], context, supplier);
                LM.mexxImportArticleInfoInvoice.execute(outputListInOrder[1], context, supplier);
                LM.mexxImportColorInvoice.execute(outputListInOrder[2], context, supplier);
                LM.mexxImportPricesInvoice.execute(outputListInOrder[3], context, supplier);
            }
            context.addAction(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception
                e) {
            throw new RuntimeException(e);
        }


    }
}
