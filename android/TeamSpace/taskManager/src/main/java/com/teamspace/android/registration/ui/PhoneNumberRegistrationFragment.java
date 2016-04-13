package com.teamspace.android.registration.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.teamspace.android.R;
import com.teamspace.android.caching.DataManager;
import com.teamspace.android.caching.DataManagerCallback;
import com.teamspace.android.common.ui.DebugActivity;
import com.teamspace.android.common.ui.LauncherActivity;
import com.teamspace.android.models.UserAuthData;
import com.teamspace.android.tasklist.ui.AllTasksListViewActivity;
import com.teamspace.android.utils.Constants;
import com.teamspace.android.utils.Utils;

/**
 * Created by vivek on 2/22/15.
 */
public class PhoneNumberRegistrationFragment extends Fragment {

    Button continueWithPhone;
    Button changePhone;
    ProgressDialog progress;
    private boolean mValidationMode;
    private Handler handler;
    private long startTime = 0;
    private long timeOutForValidation = 200000; // msec

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.phone_registration_fragment, container, false);

        continueWithPhone = (Button) v.findViewById(R.id.continue_button);
        String phoneNumber = Utils.getSignedInUserPhoneNumber();
        if (Utils.isStringEmpty(phoneNumber)) {
            continueWithPhone.setText(v.getContext().getString(R.string.enter_phone_str));
            continueWithPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    enterPhoneNumber(view);
                }
            });
        } else {
            continueWithPhone.setText(v.getContext().getString(R.string.continue_str) + " " + phoneNumber);
            continueWithPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    continueWithPhoneWithoutValidatingPhoneNumber(view);
                }
            });
        }

        changePhone = (Button) v.findViewById(R.id.change_phone_button);
        changePhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "This feature is not yet supported.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        Button changeServer = (Button) v.findViewById(R.id.change_server_button);
        changeServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), DebugActivity.class);
                startActivity(i);
            }
        });

        return v;
    }

    private void enterPhoneNumber(final View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle("Enter Phone Number");
        alert.setMessage("Example: +919890123456");

        // Set an EditText view to get user input
        final EditText input = new EditText(getActivity());
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                enterUserName(view, input.getText().toString());
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private void enterUserName(final View view, final String phoneNumber) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle("Enter Your Name");
        alert.setMessage("Example: Akshay Kumar");

        // Set an EditText view to get user input
        final EditText input = new EditText(getActivity());
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Utils.saveSelfPhoneNumber(phoneNumber);
                String value = input.getText().toString();
                Utils.saveSelfUserName(value);
                continueWithPhoneWithoutValidatingPhoneNumber(view);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private void continueWithPhone(final View view) {
        progress = new ProgressDialog(view.getContext());
        progress.setTitle("Validating Phone Number");
        progress.setMessage("Please wait while we automatically validate your phone number. This can take few minutes.");
        progress.setCancelable(false);
        progress.setMax(100);
        progress.show();

        DataManager dataMgr = DataManager.getInstance(view.getContext());
        dataMgr.signUpOrSignIn(new DataManagerCallback() {

            @Override
            public void onSuccess(String response) {
                startTime = System.currentTimeMillis();
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recheckForUserIdAfter1Second();
                    }
                }, 1000);
            }

            @Override
            public void onFailure(String response) {
                // Notify user about the error
                Toast.makeText(
                        view.getContext(),
                        view.getContext().getResources().getString(
                                R.string.error_registration_failed),
                        Toast.LENGTH_SHORT).show();
                if (progress != null) {
                    progress.dismiss();
                }
            }
        });
    }

    private void continueWithPhoneWithoutValidatingPhoneNumber(final View view) {
        progress = new ProgressDialog(view.getContext());
        progress.setTitle("Validating Phone Number");
        progress.setMessage("Checking in Alpha users database");
        progress.setCancelable(false);
        progress.setMax(100);
        progress.show();

        // We send #### as OTP to server along with the phone number.
        // If this phone number is in the whitelisted alpha users, the server will send back the
        // userID and Key.
        DataManager dataMgr = DataManager.getInstance(getActivity());
        dataMgr.verifyOTP("LLLL", new DataManagerCallback() {
            public void onDataReceivedFromServer(String dataStoreKey) {
                if (dataStoreKey == null) {
                    return;
                }
                UserAuthData data = (UserAuthData)
                        DataManager.getInstance(getActivity()).retrieveData(dataStoreKey);
                if (data == null) {
                    return;
                }

                Utils.setSignedInUserId(data.getUserId());
                Utils.setSignedInUserKey(data.getKey());
                // Adding some artificial delay so that user can read the display message
                // while spinner shows.
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recheckForUserIdAfter1Second();
                    }
                }, 2000);
            }

            public void onFailure(String response) {
                // Notify user about the error
                Toast.makeText(
                        view.getContext(),
                        view.getContext().getResources().getString(
                                R.string.error_registration_failed),
                        Toast.LENGTH_SHORT).show();
                if (progress != null) {
                    progress.dismiss();
                }
            }
        });
    }

    private void recheckForUserIdAfter1Second() {
        if (Utils.isStringNotEmpty(Utils.getSignedInUserId()) &&
                Utils.isStringNotEmpty(Utils.getSignedInUserKey())) {
            if (progress != null && getActivity() != null && !isRemoving()) {
                Utils.log("UserID = " + Utils.isStringNotEmpty(Utils.getSignedInUserId()));
                progress.dismiss();
            }

            // Stop listening to SMS since it has been too long since we requested OTP.
            DataManager.getInstance(getActivity()).validateOTPFromSMS = false;

            Intent i = new Intent(getActivity(), LauncherActivity.class);
            startActivity(i);

            getActivity().finish();
        } else if (System.currentTimeMillis() - startTime < timeOutForValidation) {
            // Recheck after another second
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recheckForUserIdAfter1Second();
                }
            }, 1000);
        } else {
            if (progress != null) {
                progress.dismiss();
            }

            // Stop listening to SMS since it has been too long since we requested OTP.
            DataManager.getInstance(getActivity()).validateOTPFromSMS = false;

            Toast.makeText(getActivity(), "Validation failed due to time out",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
