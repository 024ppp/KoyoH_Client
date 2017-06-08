package com.example.administrator.koyoh_client;

/**
 * Created by Administrator on 2017/05/10.
 */

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.os.Parcelable;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author jumpei
 *
 */
public class NfcWriter {

    private NfcAdapter nfcAdapter = null;
    private String errorMessage = null;

    public NfcWriter(Context context) {
        // Get default NFC adapter
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    public void enable(Activity activity, PendingIntent pendingIntent) {
        this.nfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    public void disable(Activity activity) {
        this.nfcAdapter.disableForegroundDispatch(activity);
    }

    public boolean write(Context context, Intent intent, String targetUrl) {
        boolean result = false;
        //boolean result = true;
        this.errorMessage = null;

        Tag tag = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            this.errorMessage = "Failed: Tag is null.";
            return result;
        }

        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            this.errorMessage = "Failed: Wrong tag format.";
            return result;
        }

        NdefMessage ndefMessage = this.createMessage(targetUrl);
        if (ndefMessage == null) {
            this.errorMessage = "Failed: NdefMessage is null.";
            return result;
        }

        if (this.writeMessage(ndef, ndefMessage)) {
            result = true;
        }
        return result;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    //ICタグの固有ID取得（今後使用するか？例；IDと内容をセットにしてDBに持たせる）
    public String getIdm(Intent intent) {
        String idm = null;
        StringBuffer idmByte = new StringBuffer();
        //byte[] rawIdm = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        byte[] rawIdm = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        if (rawIdm != null) {
            for (int i = 0; i < rawIdm.length; i++) {
                idmByte.append(Integer.toHexString(rawIdm[i] & 0xff));
            }
            idm = idmByte.toString();
        }
        return idm;
    }

    //ICタグに書き込まれた内容を読み取る
    public String getTagText(Intent intent) {
        String str = "";
        //NFCシールからのアクセスかチェック
        String ac = intent.getAction();

        try {
            if (ac.equals((NfcAdapter.ACTION_NDEF_DISCOVERED))) {
                //Ndefメッセージの取得
                Parcelable[] raws = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefMessage[] msgs = new NdefMessage[raws.length];

                for (int i = 0; i < raws.length; i++) {
                    msgs[i] = (NdefMessage) raws[i];
                    for (NdefRecord record : msgs[i].getRecords()) {
                        //payloadを取得
                        byte[] payload = record.getPayload();
                        //payloadが空白ならブレイク
                        if (payload == null) break;
                        int idx = 0;
                        for (byte data : payload) {
                            if (idx > 2) {
                                str += String.format("%c", data);
                            }
                            idx++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            //this.errorMessage = "Failed: " + e.getLocalizedMessage();
            str = "読み取り失敗" + e.getCause();
        }
            return str;
    }

    private NdefMessage createMessage(String targetUri) {
        NdefRecord[] rs = new NdefRecord[] {
                createUriRecord(targetUri),
                createActionRecord()
        };
        NdefMessage spPayload = new NdefMessage(rs);

        NdefRecord spRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_SMART_POSTER,
                new byte[0],
                spPayload.toByteArray());
        return new NdefMessage(new NdefRecord[]{spRecord});
    }

    private NdefRecord createUriRecord(String uri) {
        return NdefRecord.createUri(uri);
    }

    private NdefRecord createActionRecord() {
        byte[] typeField = "act".getBytes(Charset.forName("US-ASCII"));
        byte[] payload = {(byte) 0x00};
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                typeField,
                new byte[0],
                payload); // y2z
    }

    private boolean writeMessage(Ndef ndef, NdefMessage ndefMessage) {
        boolean result = false;

        if (!ndef.isWritable()) {
            this.errorMessage = "Failed: Readonly.";
            return result;
        }

        int messageSize = ndefMessage.toByteArray().length;
        if (messageSize > ndef.getMaxSize()) {
            this.errorMessage = "Failed: Overflow.";
            return false;

        }

        try {
            if (!ndef.isConnected()) {
                ndef.connect();
            }
            ndef.writeNdefMessage(ndefMessage);
            result = true;

        } catch (TagLostException e) {
            this.errorMessage = "Failed: " + e.getLocalizedMessage();

        } catch (IOException e) {
            this.errorMessage = "Failed: " + e.getLocalizedMessage();

        } catch (FormatException e) {
            this.errorMessage = "Failed: " + e.getLocalizedMessage();

        } finally {
            try {
                ndef.close();
            } catch (IOException e) {
                // ignore.
            }
        }

        return result;
    }


}
