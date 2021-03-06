package com.orlanth23.annoncesnc.utility;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.orlanth23.annoncesnc.R;
import com.orlanth23.annoncesnc.dialog.NoticeDialogFragment;
import com.orlanth23.annoncesnc.dto.CurrentUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.orlanth23.annoncesnc.database.DictionaryDAO.Dictionary;
import static com.orlanth23.annoncesnc.database.DictionaryDAO.existDictionary;
import static com.orlanth23.annoncesnc.database.DictionaryDAO.insertInto;
import static com.orlanth23.annoncesnc.database.DictionaryDAO.update;


public class Utility {

    public static final String DIALOG_TAG_UNREGISTER = "UNREGISTER";

    //Email Pattern
    private static final String EMAIL_PATTERN =
        "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public static boolean isTextViewOnError(boolean condition, TextView textView, String msgError, boolean requestFocus) {
        if (condition) {
            textView.setError(msgError);
            if (requestFocus) {
                textView.requestFocus();
            }
            return true;
        } else {
            return false;
        }
    }

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        String result = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index;
            if (cursor != null) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                result = cursor.getString(column_index);
            }

            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static Bitmap resizeBitmap(Bitmap p_bitmap, int maxPx) {
        int newWidth;
        int newHeight;

        // L'image est trop grande il faut la réduire
        if ((p_bitmap.getWidth() > maxPx) || (p_bitmap.getHeight() > maxPx)) {
            int max;
            if (p_bitmap.getWidth() > maxPx) {
                max = p_bitmap.getWidth();
            } else {
                max = p_bitmap.getHeight();
            }

            double prorata = (double) maxPx / max;

            newWidth = (int) (p_bitmap.getWidth() * prorata);
            newHeight = (int) (p_bitmap.getHeight() * prorata);
        } else {
            return p_bitmap;
        }

        return Bitmap.createScaledBitmap(p_bitmap, newWidth, newHeight, true);
    }

    public static String saveBitmap(Bitmap bitmap, String TAG) {
        // On enregistre cette nouvelle image retaillée et on récupère son chemin dans path
        String retour = null;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = Utility.getOutputMediaFile(Constants.MEDIA_TYPE_IMAGE, CurrentUser.getInstance().getIdUTI(), TAG);
        try {
            if (f != null) {
                if (f.createNewFile()) {
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(bytes.toByteArray());
                    fo.close();
                    retour = f.getPath();
                }
            }
        } catch (IOException e) {
            Log.e("saveBitmap", e.getMessage(), e);
        }

        return retour;
    }

    public static byte[] transformBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        InputStream imageStream;
        Bitmap bitmap = null;
        try {
            imageStream = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(imageStream);
        } catch (FileNotFoundException e) {
            Log.e("getBitmapFromUri", e.getMessage(), e);
        }
        return bitmap;
    }

    public static boolean validateEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches() && email.contains("@");
    }

    public static boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    public static boolean isNotNull(String txt) {
        return txt != null && txt.trim().length() > 0;
    }

    private static boolean isWifiActivated(Context context) {
        // Test de la connexion WIFI
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private static boolean is3GActivated(Context context) {
        // Test de la connexion 3g
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    private static int getColorFromString(String color) {
        String colorComplete = "#".concat(color);
        return Color.parseColor(colorComplete);
    }

    public static int getColorFromInteger(Integer color) {
        String colorString = color.toString();
        return getColorFromString(colorString);
    }

    // Récupération des préférences du nombre de caractère
    public static int getPrefNumberCar(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String number = sharedPrefs.getString("pref_key_number_car", context.getResources().getString(R.string.pref_default_number_car));
        return Integer.valueOf(number);
    }

    /**
     * @param fragmentManager Get from the context
     * @param message         The message to be send
     * @param type            From NoticeDialogFragment
     * @param img             From NoticeDialogFragment
     * @param tag             A text to be a TAG
     */
    public static void SendDialogByFragmentManager(FragmentManager fragmentManager, String message, int type, int img, @Nullable String tag) {
        NoticeDialogFragment dialogErreur = new NoticeDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NoticeDialogFragment.P_MESSAGE, message);
        bundle.putInt(NoticeDialogFragment.P_TYPE, type);
        bundle.putInt(NoticeDialogFragment.P_IMG, img);
        dialogErreur.setArguments(bundle);
        dialogErreur.show(fragmentManager, tag);
    }

    // Envoi d'un message
    public static void SendDialogByActivity(Activity activity, String message, int type, int img, String tag) {
        SendDialogByFragmentManager(activity.getFragmentManager(), message, type, img, tag);
    }

    /**
     * Créer un URI pour stocker l'image/la video
     */
    public static Uri getOutputMediaFileUri(int type, String preName, String TAG) {
        return Uri.fromFile(getOutputMediaFile(type, preName, TAG));
    }

    /**
     * retourne le nom d'une image / d'une video
     */
    public static File getOutputMediaFile(int type, String preName, String TAG) {
        // External sdcard location
        File mediaStorageDir = new File(
            Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Constants.IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create "
                    + Constants.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS",
            Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == Constants.MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + String.valueOf(preName) + "_IMG_" + timeStamp + ".jpg");
        } else if (type == Constants.MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + String.valueOf(preName) + "_VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static String convertDate(String dateYMDHM) {
        SimpleDateFormat originalDateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.FRENCH);
        SimpleDateFormat newDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
        Date dateOriginal = null;
        try {
            dateOriginal = originalDateFormat.parse(dateYMDHM);
        } catch (ParseException e) {
            Log.e("convertDate", e.getMessage());
        }
        return newDateFormat.format(dateOriginal);
    }

    public static String convertPrice(Integer prix) {
        return NumberFormat.getNumberInstance(Locale.FRENCH).format(prix) + " " + Constants.CURRENCY;
    }

    public static boolean checkWifiAndMobileData(Context context) {
        return (Utility.isWifiActivated(context) || Utility.is3GActivated(context));
    }

    public static void hideKeyboard(Context ctx) {
        InputMethodManager inputManager = (InputMethodManager) ctx
            .getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = ((Activity) ctx).getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static boolean saveAutoComplete(Context context, String idUser, String emailUser, String displayNameUser, String telephoneUser, String password) {
        boolean retourEmail;
        boolean retourDisplayName;
        boolean retourPassword;
        boolean retourAutoConnect;
        boolean retourIdUser;
        boolean retourTelephoneUser;

        if (existDictionary(context, Dictionary.DB_CLEF_EMAIL)) {
            retourEmail = update(context, Dictionary.DB_CLEF_EMAIL, emailUser);
        } else {
            retourEmail = insertInto(context, Dictionary.DB_CLEF_EMAIL, emailUser);
        }

        // Encryptage du mot de passe
        String motDePasseEncrypted = PasswordEncryptionService.desEncryptIt(password);
        if (existDictionary(context, Dictionary.DB_CLEF_MOT_PASSE)) {
            retourPassword = update(context, Dictionary.DB_CLEF_MOT_PASSE, motDePasseEncrypted);
        } else {
            retourPassword = insertInto(context, Dictionary.DB_CLEF_MOT_PASSE, motDePasseEncrypted);
        }

        if (existDictionary(context, Dictionary.DB_CLEF_AUTO_CONNECT)) {
            retourAutoConnect = update(context, Dictionary.DB_CLEF_AUTO_CONNECT, "O");
        } else {
            retourAutoConnect = insertInto(context, Dictionary.DB_CLEF_AUTO_CONNECT, "O");
        }

        if (existDictionary(context, Dictionary.DB_CLEF_ID_USER)) {
            retourIdUser = update(context, Dictionary.DB_CLEF_ID_USER, idUser);
        } else {
            retourIdUser = insertInto(context, Dictionary.DB_CLEF_ID_USER, idUser);
        }

        if (existDictionary(context, Dictionary.DB_CLEF_TELEPHONE)) {
            retourTelephoneUser = update(context, Dictionary.DB_CLEF_TELEPHONE, telephoneUser);
        } else {
            retourTelephoneUser = insertInto(context, Dictionary.DB_CLEF_TELEPHONE, telephoneUser);
        }

        if (existDictionary(context, Dictionary.DB_CLEF_DISPLAY_NAME)) {
            retourDisplayName = update(context, Dictionary.DB_CLEF_DISPLAY_NAME, displayNameUser);
        } else {
            retourDisplayName = insertInto(context, Dictionary.DB_CLEF_DISPLAY_NAME, displayNameUser);
        }

        return retourEmail && retourPassword && retourAutoConnect && retourIdUser && retourTelephoneUser && retourDisplayName;
    }
}
