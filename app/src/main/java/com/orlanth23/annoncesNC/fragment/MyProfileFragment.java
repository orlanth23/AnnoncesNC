package com.orlanth23.annoncesnc.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.orlanth23.annoncesnc.R;
import com.orlanth23.annoncesnc.activity.ChangePasswordActivity;
import com.orlanth23.annoncesnc.dialog.NoticeDialogFragment;
import com.orlanth23.annoncesnc.dto.CurrentUser;
import com.orlanth23.annoncesnc.interfaces.CustomActivityInterface;
import com.orlanth23.annoncesnc.utility.Utility;
import com.orlanth23.annoncesnc.webservice.Proprietes;
import com.orlanth23.annoncesnc.webservice.RetrofitService;
import com.orlanth23.annoncesnc.webservice.ReturnWS;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.orlanth23.annoncesnc.utility.Utility.SendDialogByActivity;

public class MyProfileFragment extends Fragment{

    public static final String tag = MyProfileFragment.class.getName();
    public final static int CODE_CHANGE_PASSWORD = 600;

    @BindView(R.id.emailMyProfile)
    EditText emailMyProfile;
    @BindView(R.id.telephoneMyProfile)
    EditText telephoneMyProfile;
    @BindView(R.id.action_save_change)
    Button action_save_change;
    @BindView(R.id.action_desinscrire)
    Button action_desinscrire;
    @BindView(R.id.action_change_password)
    Button action_change_password;
    @BindView(R.id.action_deconnexion)
    Button action_deconnexion;

    private RetrofitService retrofitService;
    private String newEmail;
    private Integer newTelephone;
    private Callback<ReturnWS> callback = new Callback<ReturnWS>() {
        @Override
        public void onResponse(Call<ReturnWS> call, Response<ReturnWS> response) {
            if (response.isSuccessful()) {
                ReturnWS rs = response.body();
                if (rs.statusValid()) {
                    CurrentUser.getInstance().setEmailUTI(newEmail);
                    CurrentUser.getInstance().setTelephoneUTI(newTelephone);
                    Toast.makeText(getActivity(), getString(R.string.dialog_update_user_succeed), Toast.LENGTH_LONG).show();

                    View view = getView();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    getFragmentManager().popBackStackImmediate();
                } else {
                    Toast.makeText(getActivity(), rs.getMsg(), Toast.LENGTH_LONG).show();

                    // Le web service a échoué, on réactive quand même le bouton
                    action_save_change.setEnabled(true);
                }
            }
        }

        @Override
        public void onFailure(Call<ReturnWS> call, Throwable t) {
            Toast.makeText(getActivity(), getString(R.string.dialog_failed_webservice), Toast.LENGTH_LONG).show();
        }
    };

    public static MyProfileFragment newInstance() {
        return new MyProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my_profile, container, false);
        ButterKnife.bind(this, rootView);

        emailMyProfile.setText(CurrentUser.getInstance().getEmailUTI());
        telephoneMyProfile.setText(String.valueOf(CurrentUser.getInstance().getTelephoneUTI()));

        // Changement de couleur de l'action bar et du titre pour prendre celle de la catégorie
        getActivity().setTitle(getString(R.string.action_profile));
        if (getActivity() instanceof CustomActivityInterface) {
            CustomActivityInterface myCustomActivity = (CustomActivityInterface) getActivity();
            int color = ContextCompat.getColor(getActivity(), R.color.ColorPrimary);
            myCustomActivity.changeColorToolBar(color);
        }

        retrofitService = new Retrofit.Builder().baseUrl(Proprietes.getServerEndpoint()).addConverterFactory(GsonConverterFactory.create()).build().create(RetrofitService.class);

        // Création d'un listener pour se déconnecter
        View.OnClickListener onClickListenerDeconnexion = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CurrentUser.getInstance().setIdUTI(null);
                CurrentUser.getInstance().setEmailUTI(null);
                CurrentUser.getInstance().setTelephoneUTI(null);
                CurrentUser.getInstance().setConnected(false);

                // Changement de couleur de l'action bar et du titre pour prendre celle de la catégorie
                Activity myActivity = getActivity();
                if (myActivity instanceof CustomActivityInterface) {
                    CustomActivityInterface myCustomActivity = (CustomActivityInterface) myActivity;
                    myCustomActivity.refreshMenu();
                }

                getFragmentManager().popBackStackImmediate();
            }
        };

        // Création du listener pour se désinscrire
        View.OnClickListener onClickListenerChangePassword = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), ChangePasswordActivity.class);
                startActivityForResult(intent, CODE_CHANGE_PASSWORD);
            }
        };


        // Création du listener pour se désinscrire
        View.OnClickListener onClickListenerUnregister = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // On va envoyer un message de confirmation à l'utilisateur
                SendDialogByActivity(getActivity(), getString(R.string.dialog_confirm_unregister), NoticeDialogFragment.TYPE_BOUTON_YESNO, NoticeDialogFragment.TYPE_IMAGE_ERROR, Utility.DIALOG_TAG_UNREGISTER);
                Utility.hideKeyboard(getActivity());
            }
        };

        // Création d'un listener pour la mise à jour
        View.OnClickListener onClickListenerSave = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkUpdate()){

                    // When Email entered is invalid
                    String email = emailMyProfile.getText().toString();
                    if (!Utility.validateEmail(email)) {
                        emailMyProfile.setError(getString(R.string.error_invalid_email));
                        emailMyProfile.requestFocus();
                    }else{

                        // On désactive le bouton, le temps qu'on récupère une réponse
                        action_save_change.setEnabled(false);

                        newEmail = emailMyProfile.getText().toString();
                        newTelephone = Integer.valueOf(telephoneMyProfile.getText().toString());
                        Call<ReturnWS> call = retrofitService.updateUser(CurrentUser.getInstance().getIdUTI(), newEmail, newTelephone);
                        call.enqueue(callback);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.dialog_no_update, Toast.LENGTH_LONG).show();
                }
            }
        };

        // On attribue le onClickListener à notre bouton
        action_save_change.setOnClickListener(onClickListenerSave);
        action_desinscrire.setOnClickListener(onClickListenerUnregister);
        action_change_password.setOnClickListener(onClickListenerChangePassword);
        action_deconnexion.setOnClickListener(onClickListenerDeconnexion);

        return rootView;
    }

    private boolean checkUpdate(){
        boolean retour = false;
        String email = emailMyProfile.getText().toString();

        if (!email.equals(CurrentUser.getInstance().getEmailUTI())){
            retour = true;
        }

        if (!telephoneMyProfile.getText().toString().equals(String.valueOf(CurrentUser.getInstance().getTelephoneUTI()))){
            retour = true;
        }

        return retour;
    }
}
