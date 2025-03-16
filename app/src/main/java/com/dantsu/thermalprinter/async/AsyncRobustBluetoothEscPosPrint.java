package com.dantsu.thermalprinter.async;

import android.content.Context;
import android.os.AsyncTask;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.RobustBluetoothConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.thermalprinter.R;

public class AsyncRobustBluetoothEscPosPrint extends AsyncTask<AsyncEscPosPrinter, Integer, Integer> {
    protected final static int FINISH_SUCCESS = 1;
    protected final static int FINISH_NO_PRINTER = 2;
    protected final static int FINISH_PRINTER_DISCONNECTED = 3;
    protected final static int FINISH_PARSER_ERROR = 4;
    protected final static int FINISH_ENCODING_ERROR = 5;
    protected final static int FINISH_BARCODE_ERROR = 6;

    protected final static int PROGRESS_CONNECTING = 1;
    protected final static int PROGRESS_CONNECTED = 2;
    protected final static int PROGRESS_PRINTING = 3;
    protected final static int PROGRESS_PRINTED = 4;

    protected Context context;
    protected OnPrintFinished onPrintFinished;

    public AsyncRobustBluetoothEscPosPrint(Context context) {
        this.context = context;
    }

    public AsyncRobustBluetoothEscPosPrint(Context context, OnPrintFinished onPrintFinished) {
        this.context = context;
        this.onPrintFinished = onPrintFinished;
    }

    protected Integer doInBackground(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return AsyncRobustBluetoothEscPosPrint.FINISH_NO_PRINTER;
        }

        publishProgress(AsyncRobustBluetoothEscPosPrint.PROGRESS_CONNECTING);

        AsyncEscPosPrinter printerData = printersData[0];
        DeviceConnection deviceConnection = printerData.getPrinterConnection();

        if (deviceConnection == null) {
            return AsyncRobustBluetoothEscPosPrint.FINISH_NO_PRINTER;
        }

        try {
            // Use RobustBluetoothConnection if available
            if (deviceConnection instanceof RobustBluetoothConnection) {
                RobustBluetoothConnection robustConnection = (RobustBluetoothConnection) deviceConnection;
                robustConnection.connect();
            } else {
                // Fall back to standard connection method
                deviceConnection.connect();
            }

            publishProgress(AsyncRobustBluetoothEscPosPrint.PROGRESS_CONNECTED);

            EscPosPrinter printer = new EscPosPrinter(
                    deviceConnection,
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine(),
                    new EscPosCharsetEncoding("CP437", 16)
            );

            publishProgress(AsyncRobustBluetoothEscPosPrint.PROGRESS_PRINTING);

            String[] textsToPrint = printerData.getTextsToPrint();

            for (String textToPrint : textsToPrint) {
                printer.printFormattedTextAndCut(textToPrint);
                Thread.sleep(500);
            }

            publishProgress(AsyncRobustBluetoothEscPosPrint.PROGRESS_PRINTED);

        } catch (EscPosConnectionException e) {
            e.printStackTrace();
            return AsyncRobustBluetoothEscPosPrint.FINISH_PRINTER_DISCONNECTED;
        } catch (EscPosParserException e) {
            e.printStackTrace();
            return AsyncRobustBluetoothEscPosPrint.FINISH_PARSER_ERROR;
        } catch (EscPosEncodingException e) {
            e.printStackTrace();
            return AsyncRobustBluetoothEscPosPrint.FINISH_ENCODING_ERROR;
        } catch (EscPosBarcodeException e) {
            e.printStackTrace();
            return AsyncRobustBluetoothEscPosPrint.FINISH_BARCODE_ERROR;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncRobustBluetoothEscPosPrint.FINISH_PRINTER_DISCONNECTED;
        }

        return AsyncRobustBluetoothEscPosPrint.FINISH_SUCCESS;
    }

    protected void onProgressUpdate(Integer... progress) {
        if (this.onPrintFinished != null) {
            switch (progress[0]) {
                case AsyncRobustBluetoothEscPosPrint.PROGRESS_CONNECTING:
                    this.onPrintFinished.onConnecting();
                    break;
                case AsyncRobustBluetoothEscPosPrint.PROGRESS_CONNECTED:
                    this.onPrintFinished.onConnected();
                    break;
                case AsyncRobustBluetoothEscPosPrint.PROGRESS_PRINTING:
                    this.onPrintFinished.onPrinting();
                    break;
                case AsyncRobustBluetoothEscPosPrint.PROGRESS_PRINTED:
                    this.onPrintFinished.onPrinted();
                    break;
            }
        }
    }

    protected void onPostExecute(Integer result) {
        if (this.onPrintFinished != null) {
            switch (result) {
                case AsyncRobustBluetoothEscPosPrint.FINISH_SUCCESS:
                    this.onPrintFinished.onSuccess();
                    break;
                case AsyncRobustBluetoothEscPosPrint.FINISH_NO_PRINTER:
                    this.onPrintFinished.onError(this.context.getString(R.string.no_printer_found));
                    break;
                case AsyncRobustBluetoothEscPosPrint.FINISH_PRINTER_DISCONNECTED:
                    this.onPrintFinished.onError(this.context.getString(R.string.printer_disconnected));
                    break;
                case AsyncRobustBluetoothEscPosPrint.FINISH_PARSER_ERROR:
                    this.onPrintFinished.onError(this.context.getString(R.string.invalid_formatted_text));
                    break;
                case AsyncRobustBluetoothEscPosPrint.FINISH_ENCODING_ERROR:
                    this.onPrintFinished.onError(this.context.getString(R.string.invalid_charset));
                    break;
                case AsyncRobustBluetoothEscPosPrint.FINISH_BARCODE_ERROR:
                    this.onPrintFinished.onError(this.context.getString(R.string.invalid_barcode));
                    break;
            }
        }
    }

    public interface OnPrintFinished {
        void onError(String errorMessage);

        void onSuccess();

        void onConnecting();

        void onConnected();

        void onPrinting();

        void onPrinted();
    }
} 