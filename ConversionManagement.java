public class ConversionManagement implements ConversionManagementInterface {

    private int cores = 1;
    private ConverterInterface converter;
    private ConversionReceiverInterface receiver;

    public void setCores(int cores){
        this.cores = cores;
    }

    public void setConverter(ConverterInterface converter){
        this.converter = converter;
    }

	public void setConversionReceiver(ConversionReceiverInterface receiver){
        this.receiver = receiver;
    }

    public void addDataPortion(ConverterInterface.DataPortionInterface data){
        System.out.println(data.id());
    }

}