package com.orlanth23.annoncesNC.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.orlanth23.annoncesNC.R;
import com.orlanth23.annoncesNC.dialogs.NoticeDialogFragment;
import com.orlanth23.annoncesNC.dto.CurrentUser;
import com.orlanth23.annoncesNC.dto.Utilisateur;
import com.orlanth23.annoncesNC.utility.PasswordEncryptionService;
import com.orlanth23.annoncesNC.utility.Utility;
import com.orlanth23.annoncesNC.webservices.AccessPoint;
import com.orlanth23.annoncesNC.webservices.RetrofitService;
import com.orlanth23.annoncesNC.webservices.ReturnWS;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.orlanth23.annoncesNC.utility.Utility.SendDialogByFragmentManager;

/**
 * Register Activity Class
 */
public class RegisterActivity extends AppCompatActivity {

    public static final int CODE_REGISTER_ACTIVITY = 100;
    private static final String tag = RegisterActivity.class.getName();
    @BindView(R.id.register_error)
    TextView errorMsg;
    @BindView(R.id.registerTelephone)
    EditText telephoneET;
    @BindView(R.id.registerEmail)
    EditText emailET;
    @BindView(R.id.registerPassword)
    EditText pwdET;
    @BindView(R.id.registerPasswordConfirm)
    TextView pwdConfirmET;
    @BindView(R.id.checkBox_register_remember_me)
    CheckBox checkBox_register_remember_me;
    private ProgressDialog prgDialog;
    private AppCompatActivity mActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        // Rajout d'une toolbar et changement du titre
        ActionBar tb = getSupportActionBar();
        if (tb != null) {
            tb.setTitle(R.string.action_sign_up);
            tb.setDisplayHomeAsUpEnabled(true);
        }

        // Création d'une progress bar
        prgDialog = new ProgressDialog(this);
        prgDialog.setMessage(getString(R.string.dialog_msg_patience));
        prgDialog.setCancelable(false);
    }

    /**
     * Set degault values for Edit View controls
     */
    public void setDefaultValues() {
        emailET.setText("");
        pwdET.setText("");
        pwdConfirmET.setText("");
        telephoneET.setText("");
    }

    private boolean checkRegister() {
        Integer telephone = 0;
        View focusView = null;
        boolean cancel = false;

        // Récupération des variables présentes sur le layout
        // Téléphone, email et password
        String monTelephone = telephoneET.getText().toString();
        if (!monTelephone.equals("")){
            telephone = Integer.parseInt(monTelephone);
        }
        String email = emailET.getText().toString().replace("'", "''");
        String password = pwdET.getText().toString().replace("'", "''");
        String passwordConfirm = pwdConfirmET.getText().toString().replace("'", "''");

        // When Email Edit View and Password Edit View have values other than Null
        if (!Utility.isNotNull(email)) {
            emailET.setError(getString(R.string.error_field_required));
            focusView = emailET;
            cancel = true;
        }

        if (!Utility.isNotNull(String.valueOf(telephone))) {
            telephoneET.setError(getString(R.string.error_field_required));
            focusView = telephoneET;
            cancel = true;
        }

        if (!Utility.isNotNull(password)) {
            pwdET.setError(getString(R.string.error_field_required));
            focusView = pwdET;
            cancel = true;
        }

        if (!Utility.isNotNull(passwordConfirm)) {
            pwdConfirmET.setError(getString(R.string.error_field_required));
            focusView = pwdConfirmET;
            cancel = true;
        }

        // When Email entered is invalid
        if (!Utility.validateEmail(email)) {
            emailET.setError(getString(R.string.error_invalid_email));
            focusView = emailET;
            cancel = true;
        }


        if (!password.equals(passwordConfirm)) {
            pwdConfirmET.setError(getString(R.string.error_incorrect_password));
            focusView = pwdConfirmET;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        }

        return !cancel;
    }

    /**
     * Method gets triggered when Register button is clicked
     *
     * @param view
     */
    public void register(View view) {

        int telephone = 0;

        if (checkRegister()) {

            // Récupération des variables présentes sur le layout
            // Nom, Prenom, Téléphone, email et password
            String monTelephone = telephoneET.getText().toString();
            if (!monTelephone.equals("")) {
                telephone = Integer.parseInt(monTelephone);
            }
            String email = emailET.getText().toString().replace("'", "''");
            String password = pwdET.getText().toString().replace("'", "''");
            final String motDePasseEncrypted = PasswordEncryptionService.desEncryptIt(password);

            // Appel du RETROFIT Webservice
            RetrofitService retrofitService = new RestAdapter.Builder().setEndpoint(AccessPoint.getInstance().getServerEndpoint()).build().create(RetrofitService.class);
            retrofit.Callback<ReturnWS> myCallback = new retrofit.Callback<ReturnWS>() {
                @Override
                public void success(ReturnWS rs, Response response) {
                    prgDialog.hide();
                    if (rs.statusValid()) {

                        // Si on a coché la case pour se souvenir de l'utilisateur
                        if (checkBox_register_remember_me.isChecked()) {
                            Utility.saveAutoComplete(mActivity, emailET, pwdET, checkBox_register_remember_me);
                        }

                        // On remet les zones à blank
                        setDefaultValues();

                        // Display successfully registered message using Toast
                        Toast.makeText(mActivity, getString(R.string.dialog_register_ok), Toast.LENGTH_LONG).show();

                        // Récupération de l'utilisateur
                        Gson gson = new Gson();

                        Utilisateur user = gson.fromJson(rs.getMsg(), Utilisateur.class);

                        // Récupération de l'utilisateur comme étant l'utilisateur courant
                        CurrentUser.getInstance().setIdUTI(user.getIdUTI());
                        CurrentUser.getInstance().setEmailUTI(user.getEmailUTI());
                        CurrentUser.getInstance().setTelephoneUTI(user.getTelephoneUTI());
                        CurrentUser.setConnected(true);

                        Utility.hideKeyboard(mActivity);

                        Intent returnIntent = new Intent();
                        setResult(RESULT_OK, returnIntent);                             // On retourne un résultat RESULT_OK
                        finish();                                                       // On finit l'activité
                    } else {
                        errorMsg.setText(rs.getMsg());
                        Toast.makeText(mActivity, rs.getMsg(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    prgDialog.hide();
                    SendDialogByFragmentManager(getFragmentManager(), getString(R.string.dialog_failed_webservice), NoticeDialogFragment.TYPE_BOUTON_OK, NoticeDialogFragment.TYPE_IMAGE_ERROR, tag);
                }
            };
            prgDialog.show();

            retrofitService.register(email, motDePasseEncrypted, telephone, myCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        prgDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prgDialog.dismiss();
    }
}
