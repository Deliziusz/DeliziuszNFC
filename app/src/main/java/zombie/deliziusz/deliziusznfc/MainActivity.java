package zombie.deliziusz.deliziusznfc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    public static final String Error_Detected = "No se detecta el TAG NFC";
    public static final String Write_Success = "Texto Escrito Correctamente";
    public static final String Write_Error = "ERROR al escribir en el TAG NFC";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writingTagFilters[];
    boolean writeMode;
    Tag myTag;
    Context context;
    TextView edit_message;
    TextView nfc_contents;
    Button ActivateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edit_message = (TextView) findViewById(R.id.edit_message);
        nfc_contents = (TextView) findViewById(R.id.nfc_contents);
        ActivateButton = findViewById(R.id.ActivateButton);
        context = this;


        ActivateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(myTag== null){
                        Toast.makeText(context, Error_Detected, Toast.LENGTH_LONG).show();
                    }else{
                        write("PlainText|"+edit_message.getText().toString(),myTag);
                        Toast.makeText(context, Write_Success, Toast.LENGTH_LONG).show();
                    }
                }catch (IOException e){
                    Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }catch (FormatException e){
                    Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }//fin del 2catch
            }//fin del onclick
        });

        nfcAdapter = nfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter==null){
            Toast.makeText(this, "Tu dispositivo no soporta NFC", Toast.LENGTH_LONG).show();
            finish();
        }
        readfromIntent(getIntent());
        pendingIntent = PendingIntent
                .getActivity(this, 0, new
                        Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[] { tagDetected};
    }

    private void readfromIntent(Intent intent) {
        String action = intent.getAction();
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null){
                msgs = new NdefMessage[rawMsgs.length];
                for(int i = 0; i < rawMsgs.length; i++){
                    msgs[i]=(NdefMessage) rawMsgs[i];
                }//fin de ciclo
            }//fin del if

            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage[] msgs) {
        if(msgs == null || msgs.length == 0) return;

        String text = "";
        //String tagId = new String(msgs[0].getRecords()[0].getType())
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEnconding = ((payload[0] & 128) == 0) ? "UTF-8":"UTF-8"; //get the text enconding
        int languageCodeLength = payload[0] & 0063; //Get the language code p.e. "en"

        try {
            //get the text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength -1, textEnconding);
        }catch (UnsupportedEncodingException e){
            Log.e("UnsupportedEnconding", e.toString());
        }

        nfc_contents.setText("NFC Content: "+text);
    }

    private void write (String text, Tag tag) throws IOException,FormatException{
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        //Get an instance of Ndef for the tag
        Ndef ndef = Ndef.get(tag);
        //Enable I/O
        ndef.connect();
        //Write the menssage
        ndef.writeNdefMessage(message);
        //Close the connection
        ndef.close();
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        //set status byte (see NDEF spec for actual bits)
        payload[0] =(byte) langLength;

        //copy langbytes and textbytes into playload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        writeModeOff();
    }


    @Override
    protected void onResume() {
        super.onResume();
        writeModeOn();
    }


    /************************* Enable Write ***************************************
    *****************************************************************/
    private void writeModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writingTagFilters, null);
    }

    /**************************** Disable Write ************************************
     *****************************************************************/
    private void writeModeOff() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }
}