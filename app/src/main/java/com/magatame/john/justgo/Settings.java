package com.magatame.john.justgo;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.DecimalFormat;


public class Settings extends Fragment {

    private EditText fullNameEdt, ageEdt, kgEdt, cmEdt;
    private ToggleButton genderTBtn;
    private Button saveBtn;
    private Context ctx;
    private String full_name, age, weight, height;
    int gender;

    public Settings() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        //Populate object view IDs
        fullNameEdt = (EditText) rootView.findViewById(R.id.nameEdt);
        ageEdt = (EditText) rootView.findViewById(R.id.ageEdt);
        genderTBtn = (ToggleButton) rootView.findViewById(R.id.genderTBtn);
        kgEdt = (EditText) rootView.findViewById(R.id.kgEdt);
        cmEdt = (EditText) rootView.findViewById(R.id.cmEdt);
        saveBtn = (Button) rootView.findViewById(R.id.saveBtn);

        ctx = rootView.getContext();
        //final due to being accessed within an inner class
        final DatabaseOperations dbOp = new DatabaseOperations(ctx);

        retrieveDetails(dbOp);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //retrieve field values
                full_name = fullNameEdt.getText().toString();
                age = ageEdt.getText().toString();
                weight = kgEdt.getText().toString();
                height = cmEdt.getText().toString();
                gender = (genderTBtn.isChecked()) ? 1 : 0;

                boolean validationAge, validationWeight, validationHeight;

                // Age - Average age children start full walking and old person alive
                validationAge = validateNumberInput(age, 2, 127);

                // Weight - Lightest person in the world and heaviest
                validationWeight = validateNumberInput(weight, 27, 470);

                // Height - Smallest and largest person in the world
                validationHeight = validateNumberInput(height, 55, 252);

                //verify all the required fields have been entered
                if (full_name.isEmpty() || age.isEmpty() || weight.isEmpty() || height.isEmpty()) {
                    Toast.makeText(getActivity(), "Please enter your details", Toast.LENGTH_LONG).show();
                } else if (validationAge || validationWeight || validationHeight) {
                    // Don't do anything since we'll already have handle it.
                } else {

                    // Close keyboard popup only if it's open
                    if (getActivity().getCurrentFocus() != null) {
                        InputMethodManager inputManager = (InputMethodManager)
                                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                                InputMethodManager.HIDE_NOT_ALWAYS);
                    }

                    //check if user is registering
                    if (!dbOp.getUserDetails().moveToFirst()) {
                        //re-enable swipe/action bar tabs
                        ((JustGo) getActivity()).firstTimeUser(false);
                        dbOp.putUserDetails(full_name, Integer.parseInt(age), gender, roundTwoDecimals(weight), roundTwoDecimals(height));
                        Toast.makeText(getActivity(), "Registration successful", Toast.LENGTH_LONG).show();
                    } else { //or updating
                        dbOp.updateUserDetails(fullNameEdt.getText().toString(), full_name, Integer.parseInt(age), gender, roundTwoDecimals(weight), roundTwoDecimals(height));
                        Toast.makeText(getActivity(), "Update successful", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Inflate the layout for this fragment
        return rootView;
    }

    private double roundTwoDecimals(String d) {
        double decimal = Double.parseDouble(d);;
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(decimal));
    }

    private boolean validateNumberInput(String currentLength, double minimum, double maximum) {
        double value;
        // Avoids null integer crash
        if (currentLength.isEmpty()) {
            value = 0;
        } else {
            // Parse our string to integer
            value = Double.parseDouble(currentLength);
        }
        if (value > 0 && value <= minimum) {
            Toast.makeText(getActivity(), "The minimum value for this field is " + minimum, Toast.LENGTH_LONG).show();
            return true;
        } else if (value >= maximum) {
            Toast.makeText(getActivity(), "The maximum value for this field is " + maximum, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    //retrieves the users details from the database.
    public void retrieveDetails(DatabaseOperations dbOp) {
        Cursor CR = dbOp.getUserDetails();

        //Only set the values if there's data in the database.
        if (CR.moveToFirst()) /* returns true if exists */ {
            fullNameEdt.setText(CR.getString(TableData.UserDetailOrdinals.FULL_NAME.ordinal()));
            ageEdt.setText(CR.getString(TableData.UserDetailOrdinals.AGE.ordinal()));
            kgEdt.setText(CR.getString(TableData.UserDetailOrdinals.WEIGHT.ordinal()));
            cmEdt.setText(CR.getString(TableData.UserDetailOrdinals.HEIGHT.ordinal()));
            genderTBtn.setChecked(CR.getInt(TableData.UserDetailOrdinals.GENDER.ordinal()) > 0);
        }
    }
}
