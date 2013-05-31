package com.jonathanhester.fb_demo;

import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;
import com.facebook.widget.WebDialog;

public class FbDemoActivity extends FragmentActivity {
	private Button showFriendsButton;
	private Button showPhotosButton;
	private Button postStatusUpdateButton;
	private Button postCustomActionButton;
	private ProfilePictureView profilePictureView;
	private TextView greeting;
	private ViewGroup controlsContainer;
	private GraphUser user;
	private PendingAction pendingAction = PendingAction.NONE;
	private WebDialog dialog;
	private ProgressDialog spinner;

	private static final List<String> PERMISSIONS = Arrays
			.asList("publish_actions");

	private static final List<String> STREAM_PERMISSIONS = Arrays
			.asList("read_stream");

	private static final List<String> PUBLIST_PERMISSIONS = Arrays
			.asList("publish_actions");

	private enum PendingAction {
		NONE, VIEW_PHOTOS, OPEN_GRAPH_POST
	}

	private UiLifecycleHelper uiHelper;

	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        spinner = new ProgressDialog(this);
        spinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        spinner.setMessage(getString(R.string.com_facebook_loading));


		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		setContentView(R.layout.activity_fb_demo);

		profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
		greeting = (TextView) findViewById(R.id.greeting);

		showFriendsButton = (Button) findViewById(R.id.showFriendsButton);
		showFriendsButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				onClickShowFriends();
			}
		});

		showPhotosButton = (Button) findViewById(R.id.showPhotosButton);
		showPhotosButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				onClickShowPhotos();
			}
		});

		postStatusUpdateButton = (Button) findViewById(R.id.postStatusUpdateButton);
		postStatusUpdateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				onClickPostStatusUpdate();
			}
		});

		postCustomActionButton = (Button) findViewById(R.id.postCustomActionButton);
		postCustomActionButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				onClickPostCustomAction();
			}
		});

		controlsContainer = (ViewGroup) findViewById(R.id.main_ui_container);

		final FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentById(R.id.fragment_container);
		if (fragment != null) {
			// If we're being re-created and have a fragment, we need to a) hide
			// the main UI controls and
			// b) hook up its listeners again.
			controlsContainer.setVisibility(View.GONE);
		}

		// Listen for changes in the back stack so we know if a fragment got
		// popped off because the user
		// clicked the back button.
		fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
				if (fm.getBackStackEntryCount() == 0) {
					// We need to re-show our UI.
					controlsContainer.setVisibility(View.VISIBLE);
				}
			}
		});

		updateUI();

		// start Facebook Login
		Session.openActiveSession(this, true, new Session.StatusCallback() {

			@Override
			public void call(Session session, SessionState state,
					Exception exception) {
				// TODO Auto-generated method stub
				if (session.isOpened()) {

					// make request to the /me API
					Request.executeMeRequestAsync(session,
							new Request.GraphUserCallback() {

								// callback after Graph API response with user
								// object
								@Override
								public void onCompleted(GraphUser user,
										Response response) {
									if (user != null) {
										FbDemoActivity.this.user = user;
										updateUI();
									}
								}
							});
				}
			}
		});
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		uiHelper.onPause();
	}

	private void onClickShowFriends() {
		Session session = Session.getActiveSession();
		if (session != null) {
			Bundle params = new Bundle();
			params.putString("fields", "name, picture, location");

			// make request to the /me API
			spinner.show();
			new Request(session, "me/friends", params, null,
					new Request.Callback() {

						@Override
						public void onCompleted(Response response) {
							spinner.hide();
							// TODO Auto-generated method stub
							String string = response.getGraphObject()
									.getInnerJSONObject().toString();

							Intent intent = new Intent(getApplicationContext(),
									FriendsList.class);
							intent.putExtra("API_RESPONSE", string);
							intent.putExtra("METHOD", "graph");

							startActivity(intent);
						}
					}).executeAsync();
		}
	}

	private void onClickShowPhotos() {
		Session session = Session.getActiveSession();
		if (session != null) {
			if (!hasFeedPermission()) {
				// We need to get new permissions, then complete the action when
				// we get called back.
				pendingAction = PendingAction.VIEW_PHOTOS;
				NewPermissionsRequest permissionRequest = new Session.NewPermissionsRequest(
						this, STREAM_PERMISSIONS);
				permissionRequest.setCallback(callback);
				session.requestNewReadPermissions(permissionRequest);
			} else {
				showPhotos();
			}
		}
	}

	private void showPhotos() {
		Bundle params = new Bundle();
		params.putString("filter", "app_2305272732");

		Session session = Session.getActiveSession();
		
		spinner.show();
		// make request to the /me API
		new Request(session, "me/home", params, null, new Request.Callback() {

			@Override
			public void onCompleted(Response response) {
				spinner.hide();
				if (response.getGraphObject() != null) {
					// TODO Auto-generated method stub
					String string = response.getGraphObject()
							.getInnerJSONObject().toString();

					Intent intent = new Intent(getApplicationContext(),
							PhotosActivity.class);
					intent.putExtra("API_RESPONSE", string);

					startActivity(intent);
				} else {
					showMessage("Error hitting graph");
				}

			}
		}).executeAsync();
	}

	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (pendingAction != PendingAction.NONE
				&& (exception instanceof FacebookOperationCanceledException || exception instanceof FacebookAuthorizationException)) {
			new AlertDialog.Builder(FbDemoActivity.this)
					.setTitle(R.string.cancelled)
					.setMessage(R.string.permission_not_granted)
					.setPositiveButton(R.string.ok, null).show();
			pendingAction = PendingAction.NONE;
		} else if (state == SessionState.OPENED_TOKEN_UPDATED) {
			handlePendingAction();
		}
		updateUI();
	}

	@SuppressWarnings("incomplete-switch")
	private void handlePendingAction() {
		PendingAction previouslyPendingAction = pendingAction;
		// These actions may re-set pendingAction if they are still pending, but
		// we assume they
		// will succeed.
		pendingAction = PendingAction.NONE;

		switch (previouslyPendingAction) {
		case VIEW_PHOTOS:
			showPhotos();
			break;
		case OPEN_GRAPH_POST:
			postCustomAction();
		}
	}

	private void onClickPostStatusUpdate() {
		postStatusUpdate();
	}

	private void postStatusUpdate() {
		// why doesn't facebook make this get picked up by the native app?
		// Intent i = new Intent(Intent.ACTION_VIEW);
		// i.setData(Uri.parse("http://www.facebook.com/dialog/feed?app_id=" +
		// getString(R.string.app_id)));
		// startActivity(i);

		Bundle params = new Bundle();
		params.putString("caption", getString(R.string.app_name));
		params.putString("description", getString(R.string.app_desc));
		// params.putString("picture", Utility.HACK_ICON_URL);
		// params.putString("name", getString(R.string.app_action));

		showDialogWithoutNotificationBar("feed", params);
	}

	private void showDialogWithoutNotificationBar(String action, Bundle params) {
		dialog = new WebDialog.Builder(this, Session.getActiveSession(),
				action, params).setOnCompleteListener(
				new WebDialog.OnCompleteListener() {
					@Override
					public void onComplete(Bundle values,
							FacebookException error) {
						if (error != null
								&& !(error instanceof FacebookOperationCanceledException)) {
							Toast.makeText(FbDemoActivity.this,
									"Error: " + error.toString(),
									Toast.LENGTH_LONG).show();
						} else if (error instanceof FacebookOperationCanceledException) {
							Toast.makeText(FbDemoActivity.this, "Cancelled",
									Toast.LENGTH_LONG).show();
						} else if (values != null && !values.isEmpty())
							Toast.makeText(FbDemoActivity.this, "Posted!",
									Toast.LENGTH_LONG).show();
						dialog = null;
					}
				}).build();

		Window dialog_window = dialog.getWindow();
		dialog_window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		dialog.show();
	}

	private void onClickPostCustomAction() {
		Session session = Session.getActiveSession();
		if (session == null || !session.isOpened()) {
			return;
		}

		List<String> permissions = session.getPermissions();
		if (!permissions.containsAll(PERMISSIONS)) {
			pendingAction = PendingAction.OPEN_GRAPH_POST;
			requestPublishPermissions(session);
			return;
		}
		postCustomAction();
	}

	private void requestPublishPermissions(Session session) {
		if (session != null) {
			Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(
					this, PUBLIST_PERMISSIONS)
					.setDefaultAudience(SessionDefaultAudience.FRIENDS);
			session.requestNewPublishPermissions(newPermissionsRequest);
		}
	}

	private void postCustomAction() {
		Bundle fbParams = new Bundle();
		fbParams.putString("object", "http://samples.ogp.me/226075010839791");
		Request postScoreRequest = new Request(Session.getActiveSession(),
				"me/jonathanhester:click", fbParams, HttpMethod.POST,
				new Request.Callback() {
					@Override
					public void onCompleted(Response response) {
						FacebookRequestError error = response.getError();
						if (error != null) {
							showMessage("Post to Facebook failed: "
									+ error.getErrorMessage());
						} else {
							String id = (String) response.getGraphObject()
									.getProperty("id");
							showMessage("Success: id (" + id + ")");
						}
					}
				});
		Request.executeBatchAsync(postScoreRequest);
	}

	private void showMessage(String message) {
		Toast.makeText(FbDemoActivity.this, message, Toast.LENGTH_LONG).show();
	}

	private void updateUI() {
		Session session = Session.getActiveSession();
		boolean enableButtons = (session != null && session.isOpened());

		showFriendsButton.setEnabled(enableButtons);
		showPhotosButton.setEnabled(enableButtons);
		postStatusUpdateButton.setEnabled(enableButtons);
		postCustomActionButton.setEnabled(enableButtons);

		if (enableButtons && user != null) {
			profilePictureView.setProfileId(user.getId());
			greeting.setText(getString(R.string.hello_user, user.getFirstName()));
		} else {
			profilePictureView.setProfileId(null);
			greeting.setText(null);
		}
	}

	private boolean hasFeedPermission() {
		Session session = Session.getActiveSession();
		return session != null
				&& session.getPermissions().containsAll(STREAM_PERMISSIONS);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_fb_demo, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_friends_list:
			onClickShowFriends();
			return true;
		case R.id.menu_news_feed_photos:
			onClickShowPhotos();
			return true;
		case R.id.menu_post_timeline:
			onClickPostStatusUpdate();
			return true;
		case R.id.menu_post_open_graph:
			onClickPostCustomAction();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		uiHelper.onDestroy();
	}
}
