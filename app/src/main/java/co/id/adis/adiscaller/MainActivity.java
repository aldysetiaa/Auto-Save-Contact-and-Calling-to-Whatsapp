package co.id.adis.adiscaller;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText editTextPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        Button buttonCall = findViewById(R.id.buttonCall);

        buttonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveContactInBackground();
            }
        });
    }

    private void saveContactInBackground() {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();

        if (!phoneNumber.isEmpty() && PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
            String contactName = generateContactName();
            Executor executor = Executors.newSingleThreadExecutor();
            // Execute the AsyncTask for background contact saving
            new SaveContactTask(executor).execute(contactName, phoneNumber);
        } else {
            // Handle invalid phone number
            showToast("Invalid phone number");
        }
    }

    private String generateContactName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        return "Contact_" + currentDateAndTime;
    }

    private void makeCall(String phoneNumber) {
        try {
            // Create an Intent with the WhatsApp Business URI
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/62" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Log.e("WhatsAppCall", "Error initiating WhatsApp Business call", e);
            showToast("Error initiating WhatsApp Business call");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // AsyncTask for background contact saving
    private class SaveContactTask extends AsyncTask<String, Void, Boolean> {


        private final Executor executor;

        SaveContactTask(Executor executor) {
            this.executor = executor;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String contactName = params[0];
            String phoneNumber = params[1];

            try {
                return saveContact(contactName, phoneNumber);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                showToast("Contact saved successfully");
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        makeCall(editTextPhoneNumber.getText().toString().trim());
                    }
                }, 4000);
                // Now make the call or open WhatsApp on the main thread

            } else {
                showToast("Error saving contact");
            }
        }
    }

    private boolean saveContact(String contactName, String phoneNumber) {
        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            int rawContactInsertIndex = ops.size();

            // Add contact name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactName)
                    .build());

            // Add contact phone number
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            // TODO: Add more data if needed (e.g., email, organization, etc.)

            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

            // Check if the operation was successful
            return results != null && results.length > 0;
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            return false;
        }
    }
}
