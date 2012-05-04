package retail.api.remote;

import java.io.IOException;
import java.util.List;

public class TransactionCashRegisterInfo extends TransactionInfo<CashRegisterInfo> {

    public TransactionCashRegisterInfo(Integer id, String dateTimeCode, List<ItemInfo> itemsList,
                                       List<CashRegisterInfo> machineryInfoList) {
        this.id = id;
        this.dateTimeCode = dateTimeCode;
        this.itemsList = itemsList;
        this.machineryInfoList = machineryInfoList;
    }

    @Override
    public void sendTransaction(Object handler, List<CashRegisterInfo> machineryInfoList) throws IOException {
        ((CashRegisterHandler)handler).sendTransaction(this, machineryInfoList);
    }
}
