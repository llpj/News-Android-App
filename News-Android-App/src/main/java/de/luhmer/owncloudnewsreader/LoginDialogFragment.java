/**
* Android ownCloud News
*
* @author David Luhmer
* @copyright 2013 David Luhmer david-dev@live.de
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
* License as published by the Free Software Foundation; either
* version 3 of the License, or any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU AFFERO GENERAL PUBLIC LICENSE for more details.
*
* You should have received a copy of the GNU Affero General Public
* License along with this library.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package de.luhmer.owncloudnewsreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;

import java.net.MalformedURLException;
import java.net.URL;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.luhmer.owncloud.accountimporter.ImportAccountsDialogFragment;
import de.luhmer.owncloud.accountimporter.helper.AccountImporter;
import de.luhmer.owncloud.accountimporter.helper.OwnCloudAccount;
import de.luhmer.owncloud.accountimporter.interfaces.IAccountImport;
import de.luhmer.owncloudnewsreader.authentication.AuthenticatorActivity;
import de.luhmer.owncloudnewsreader.database.DatabaseConnectionOrm;
import de.luhmer.owncloudnewsreader.helper.FontHelper;
import de.luhmer.owncloudnewsreader.reader.owncloud.OwnCloudReaderMethods;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginDialogFragment extends DialogFragment implements IAccountImport {

    static LoginDialogFragment instance;
    public static LoginDialogFragment getInstance() {
        if(instance == null)
            instance = new LoginDialogFragment();
        return instance;
    }

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	private Activity mActivity;

	// Values for email and password at the time of the login attempt.
	private String mUsername;
	private String mPassword;
	private String mOc_root_path;
	private boolean mCbDisableHostnameVerification;

	// UI references.
	@InjectView(R.id.username) EditText mUsernameView;
	@InjectView(R.id.password) EditText mPasswordView;
	@InjectView(R.id.edt_owncloudRootPath) EditText mOc_root_path_View;
	@InjectView(R.id.cb_AllowAllSSLCertificates) CheckBox mCbDisableHostnameVerificationView;
    @InjectView(R.id.imgView_ShowPassword) ImageView mImageViewShowPwd;

    boolean mPasswordVisible = false;
	ProgressDialog mDialogLogin;

    @Override
    public void accountAccessGranted(OwnCloudAccount account) {
        mUsernameView.setText(account.getUsername());
        mPasswordView.setText(account.getPassword());
        mOc_root_path_View.setText(account.getUrl());
    }

    public interface LoginSuccessfullListener {
		void LoginSucceeded();
	}
	LoginSuccessfullListener listener;


	public LoginDialogFragment() {

	}

	public void setActivity(Activity mActivity) {
		this.mActivity = mActivity;
	}

	/**
	 * @param listener the listener to set
	 */
	public void setListener(LoginSuccessfullListener listener) {
		this.listener = listener;
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

		//setRetainInstance(true);

        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_signin, null);
        ButterKnife.inject(this, view);

        builder.setView(view).setTitle(getString(R.string.action_sign_in_short));

        FontHelper fHelper = new FontHelper(getActivity());
        fHelper.setFontForAllChildren(view, fHelper.getFont());


        mImageViewShowPwd.setOnClickListener(ImgViewShowPasswordListener);

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUsername = mPrefs.getString(SettingsActivity.EDT_USERNAME_STRING, null);
        mPassword = mPrefs.getString(SettingsActivity.EDT_PASSWORD_STRING, null);
        mOc_root_path = mPrefs.getString(SettingsActivity.EDT_OWNCLOUDROOTPATH_STRING, null);
        mCbDisableHostnameVerification = mPrefs.getBoolean(SettingsActivity.CB_DISABLE_HOSTNAME_VERIFICATION_STRING, false);

    	// Set up the login form.
 		mUsernameView.setText(mUsername);
 		mPasswordView.setText(mPassword);
 		mOc_root_path_View.setText(mOc_root_path);

 		mCbDisableHostnameVerificationView.setChecked(mCbDisableHostnameVerification);
 		mCbDisableHostnameVerificationView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				mPrefs.edit()
					.putBoolean(SettingsActivity.CB_DISABLE_HOSTNAME_VERIFICATION_STRING, isChecked)
					.commit();
			}
		});


        view.findViewById(R.id.btn_importAccount).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImportAccountsDialogFragment.show(getActivity(), LoginDialogFragment.this);
            }
        });

        if(AccountImporter.findAccounts(getActivity()).size() <= 0) {
            view.findViewById(R.id.btn_importAccount).setVisibility(View.GONE);
        }

        return builder.create();
    }

    private View.OnClickListener ImgViewShowPasswordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPasswordVisible = !mPasswordVisible;

            if(mPasswordVisible) {
                mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        }
    };


    @OnClick(R.id.btn_signin) void SignIn() {
        attemptLogin();
    }

    @OnClick(R.id.btn_cancel) void Cancel() {
        LoginDialogFragment.this.getDialog().cancel();
        if(mActivity instanceof AuthenticatorActivity)
            mActivity.finish();
    }

	private ProgressDialog BuildPendingDialogWhileLoggingIn()
	{
		ProgressDialog pDialog = new ProgressDialog(getActivity());
        pDialog.setTitle(getString(R.string.login_progress_signing_in));
        return pDialog;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUsernameView.setError(null);
		mPasswordView.setError(null);
		mOc_root_path_View.setError(null);

		// Store values at the time of the login attempt.
		mUsername = mUsernameView.getText().toString().trim();
		mPassword = mPasswordView.getText().toString();
		mOc_root_path = mOc_root_path_View.getText().toString().trim();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		}
		// Check for a valid email address.
		if (TextUtils.isEmpty(mUsername)) {
			mUsernameView.setError(getString(R.string.error_field_required));
			focusView = mUsernameView;
			cancel = true;
		}

		if (TextUtils.isEmpty(mOc_root_path)) {
			mOc_root_path_View.setError(getString(R.string.error_field_required));
			focusView = mOc_root_path_View;
			cancel = true;
		} else {
			try {
				URL url = new URL(mOc_root_path);
				if(!url.getProtocol().equals("https"))
					ShowAlertDialog(getString(R.string.login_dialog_title_security_warning),
							getString(R.string.login_dialog_text_security_warning), getActivity());
			} catch (MalformedURLException e) {
				mOc_root_path_View.setError(getString(R.string.error_invalid_url));
				focusView = mOc_root_path_View;
				cancel = true;
				//e.printStackTrace();
			}
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			mAuthTask = new UserLoginTask(mUsername, mPassword, mOc_root_path);
			mAuthTask.execute((Void) null);

			mDialogLogin = BuildPendingDialogWhileLoggingIn();
     	   	mDialogLogin.show();
		}
	}


	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, Integer> {
		String username;
		String password;
		String oc_root_path;
		String exception_message = "";

		public UserLoginTask(String username, String password, String oc_root_path) {
			this.username = username;
			this.password = password;
			this.oc_root_path = oc_root_path;
		}

		@Override
		protected Integer doInBackground(Void... params) {

			try {
				String _version = OwnCloudReaderMethods.GetVersionNumber(getActivity(), username, password, oc_root_path);
				if(_version != null)
				{
					_version = _version.replace(".", "");
					return  Integer.parseInt(_version);
				}

			} catch (Exception e) {
				if(e.getLocalizedMessage() != null)
					exception_message = e.getLocalizedMessage();
				else if(e instanceof NullPointerException)
					exception_message = getString(R.string.login_dialog_text_something_went_wrong);

				return -1;
			}

			return 0;
		}

		@Override
		protected void onPostExecute(final Integer versionCode) {
			mAuthTask = null;

			mDialogLogin.dismiss();

			if(versionCode == -1 && exception_message.equals("Value <!DOCTYPE of type java.lang.String cannot be converted to JSONObject")) {
				if(isAdded())
					ShowAlertDialog(getString(R.string.login_dialog_title_error), getString(R.string.login_dialog_text_not_compatible), getActivity());
			} else if(versionCode == -1) {
				if(isAdded())
					ShowAlertDialog(getString(R.string.login_dialog_title_error), exception_message, getActivity());
			} else if(versionCode == 0){
				if(isAdded())
					ShowAlertDialog(getString(R.string.login_dialog_title_error), getString(R.string.login_dialog_text_something_went_wrong), getActivity());
			} else {
				//Reset Database
				DatabaseConnectionOrm dbConn = new DatabaseConnectionOrm(getActivity());
				dbConn.resetDatabase();
				//dbConn.closeDatabase();

				//LoginFragment.this.dismiss();
				SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				Editor editor = mPrefs.edit();
				editor.putString(SettingsActivity.EDT_OWNCLOUDROOTPATH_STRING, oc_root_path);
				editor.putString(SettingsActivity.EDT_PASSWORD_STRING, password);
				editor.putString(SettingsActivity.EDT_USERNAME_STRING, username);
				editor.commit();

				if(listener != null)
					listener.LoginSucceeded();

				LoginDialogFragment.this.getDialog().cancel();
				if(mActivity instanceof AuthenticatorActivity)
					mActivity.finish();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
		}
	}

	public static void ShowAlertDialog(String title, String text, Activity activity)
	{
		AlertDialog.Builder aDialog = new AlertDialog.Builder(activity);
		aDialog.setTitle(title);
		aDialog.setMessage(text);
		aDialog.setPositiveButton(activity.getString(android.R.string.ok) , null);
		aDialog.create().show();
	}
}
