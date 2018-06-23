package com.samourai.ponydirect;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.samourai.sms.SMSSender;

import org.apache.commons.io.IOUtils;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class TxFactory {

    private static TxFactory instance = null;
    private static Context context = null;

    private static int messageIdx = 1;

    private TxFactory() { ; }

    public static TxFactory getInstance(Context ctx)   {
        context = ctx;

        if(instance == null)    {
            instance = new TxFactory();

            messageIdx = PrefsUtil.getInstance(context).getValue(PrefsUtil.MESSAGE_IDX, 1);
        }

        return instance;
    }

    public List<String> putPayload(String strHexTx)   {

        final int segment0Len = 40;
        final int segment1Len = 120;

        List<String> ret = new ArrayList<String>();

        final Transaction tx = new Transaction(MainNetParams.get(), Hex.decode(strHexTx));
        String strRaw = null;
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_Z85, false) == true)    {
            strRaw = Z85.getInstance().encode(Hex.decode(strHexTx));
        }
        else    {
            strRaw = strHexTx;
        }
        Log.d("TxFactory", "hex tx:" + strHexTx);
        Log.d("TxFactory", "hex tx Z85:" + strRaw);

        int count = 0;
        if(strRaw.length() <= segment0Len)    {
            count = 1;
        }
        else    {
            int len = strRaw.length();
            len -= segment0Len;
            count = 1;
            count += (len / segment1Len);
            if(len % segment1Len > 0)    {
                count++;
            }
        }

        int id = messageIdx;

        messageIdx++;
        if(messageIdx > 9999)    {
            messageIdx = 1;
        }
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MESSAGE_IDX, messageIdx);

        for(int i = 0; i < count; i++)   {

            try {

                JSONObject obj = new JSONObject();

                if(i == 0)    {
                    obj.put("s", count);
                    obj.put("c", i);
                    obj.put("i", id);
                    if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_MAINNET, true) == false)    {
                        obj.put("n", "t");
                    }
                    obj.put("h", tx.getHashAsString());
                    obj.put("t", strRaw.substring(0, strRaw.length() > segment0Len ? segment0Len : strRaw.length()));
                    if(strRaw.length() > segment0Len)    {
                        strRaw = strRaw.substring(segment0Len);
                    }
                }
                else    {
                    obj.put("c", i);
                    obj.put("i", id);
                    obj.put("t", strRaw.substring(0,  strRaw.length() > segment1Len ? segment1Len : strRaw.length()));
                    if(strRaw.length() > segment1Len)    {
                        strRaw = strRaw.substring(segment1Len);
                    }
                }

                ret.add(obj.toString());

            }
            catch(JSONException je) {
                ;
            }

        }

        for(String s : ret)   {
            Log.d("TxFactory", "payload:" + s);
        }

        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent("com.samourai.ponydirect.LOG");
                intent.putExtra("msg", context.getText(R.string.sending) + ":" + tx.getHashAsString() + " " + context.getText(R.string.to) + ":" + PrefsUtil.getInstance(context).getValue(PrefsUtil.SMS_RELAY, context.getText(R.string.default_relay).toString()));
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        });

        sendPayload(ret);

        return ret;
    }

    public String getPayload(List<String> payload)   {

        int count = 0;
        int id = 0;
        int idx = 0;
        String hash = null;
        String txHex = null;
        String net = null;

        for(int i = 0; i < payload.size(); i++)   {

            String s = payload.get(i);

            try {
                JSONObject obj = new JSONObject(s);

                if(i == 0)    {
                    if(!obj.has("s") || !obj.has("c") || !obj.has("i") || !obj.has("h") || !obj.has("t"))    {
                        return null;
                    }

                    idx = obj.getInt("c");
                    if(idx != i)    {
                        return null;
                    }
                    id = obj.getInt("i");
                    count = obj.getInt("s");
                    if(count != payload.size())    {
                        return null;
                    }
                    hash = obj.getString("h");
                    txHex = obj.getString("t");
                    if(obj.has("n"))    {
                        net = obj.getString("n");
                    }

                }
                else    {
                    if(!obj.has("c") || !obj.has("i") || !obj.has("t"))    {
                        return null;
                    }

                    if(id != obj.getInt("i"))    {
                        return null;
                    }
                    idx = obj.getInt("c");
                    if(idx != i)    {
                        return null;
                    }
                    txHex += obj.getString("t");

                }

            }
            catch(JSONException je) {
                je.printStackTrace();
            }

        }

        if(Z85.getInstance().isZ85(txHex))    {
            Log.d("TxFactory", "payload encoded:" + txHex);
            String _txHex = Hex.toHexString(Z85.getInstance().decode(txHex));
            Log.d("TxFactory", "payload decoded:" + _txHex);
            txHex = _txHex;
        }

        Transaction tx = new Transaction((net != null && net.equals("t")) ? TestNet3Params.get() : MainNetParams.get(), Hex.decode(txHex));
        assert(tx.getHashAsString().equalsIgnoreCase(hash));
        Log.d("TxFactory", "payload:" + tx.getHashAsString());

        return txHex;
    }

    public void sendPayload(final List<String> payload)   {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                for(int i = 0; i < payload.size(); i++)   {

                    final String s = payload.get(i);

                    final int ii = i + 1;

                    SMSSender.getInstance(context).send(s, PrefsUtil.getInstance(context).getValue(PrefsUtil.SMS_RELAY, context.getString(R.string.default_relay)));

                    try {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent("com.samourai.ponydirect.LOG");
                                intent.putExtra("msg", context.getText(R.string.sent_sms) + ", " + ii + "/" + payload.size() + ":" + s);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            }
                        });

                        Thread.sleep(5000L);
                    }
                    catch(Exception e) {
                        ;
                    }

                }

                Looper.loop();

            }
        }).start();

    }

    public void broadcastPayload(final List<String> payload, final boolean useMainNet)   {

        final String txHex = getPayload(payload);
        Log.d("TxFactory", "payload retrieved:" + txHex);

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                String response = null;
                String api = useMainNet ? "v2/pushtx/" : "test/v2/pushtx/";

                try {
                    response = postURL(null, "https://api.samouraiwallet.com/" + api, "tx=" + txHex);
                }
                catch(Exception e) {
                    Log.d("TxFactory", e.getMessage());
                    e.printStackTrace();
                    response = e.getMessage();
                }

                Log.d("TxFactory", response);

                final String _response = response;

                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent("com.samourai.ponydirect.LOG");
                        intent.putExtra("msg", context.getText(R.string.broadcasted) + ":" + _response);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                });

                Looper.loop();

            }
        }).start();

    }

    public String postURL(String contentType, String request, String urlParameters) throws Exception {

        String error = null;

        for (int ii = 0; ii < 3; ++ii) {
            URL url = new URL(request);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            try {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType);
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                connection.setUseCaches (false);

                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);

                connection.connect();

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                connection.setInstanceFollowRedirects(false);

                if (connection.getResponseCode() == 200) {
//					System.out.println("postURL:return code 200");
                    return IOUtils.toString(connection.getInputStream(), "UTF-8");
                }
                else {
                    error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
//                    System.out.println("postURL:return code " + error);
                }

                Thread.sleep(5000);
            } finally {
                connection.disconnect();
            }
        }

        return error;
    }

}
