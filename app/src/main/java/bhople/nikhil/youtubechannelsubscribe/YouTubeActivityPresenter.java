package bhople.nikhil.youtubechannelsubscribe;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.Subscription;
import com.google.api.services.youtube.model.SubscriptionSnippet;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by admin on 16-Oct-17.
 */

class YouTubeActivityPresenter {
    private final YouTubeActivityView view;
    private final YouTubeSubscribeActivity activity;

    YouTubeActivityPresenter(YouTubeActivityView view, YouTubeSubscribeActivity activity) {
        this.view = view;
        this.activity = activity;
    }

    void subscribeToYouTubeChannel(GoogleAccountCredential mCredential, String channelId) {

        new MakeRequestTask(mCredential,channelId).execute(); // creating AsyncTask for channel subscribe

    }

    private class MakeRequestTask  extends AsyncTask<Object, Object, Subscription> {
        private com.google.api.services.youtube.YouTube mService = null;
        private String channelId;

        MakeRequestTask(GoogleAccountCredential credential, String channelId) {
            this.channelId = channelId;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(activity.getResources().getString(R.string.app_name))
                    .build();
        }

        @Override
        protected Subscription doInBackground(Object... params) {
            // code for channel subscribe
            Subscription response = null;

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("part", "snippet");

            // if you could not able to import the classes then check the dependency in build.gradle
            Subscription subscription = new Subscription();
            SubscriptionSnippet snippet = new SubscriptionSnippet();
            ResourceId resourceId = new ResourceId();
            resourceId.set("channelId", channelId);
            resourceId.set("kind", "youtube#channel");

            snippet.setResourceId(resourceId);
            subscription.setSnippet(snippet);

            YouTube.Subscriptions.Insert subscriptionsInsertRequest = null;
            try {
                subscriptionsInsertRequest = mService.subscriptions().insert(parameters.get("part").toString(), subscription);
                response = subscriptionsInsertRequest.execute();
            } catch (IOException e) {
               /* if you got error message below
                "message" : "Access Not Configured. YouTube Data API has not been used in project YOUR_PROJECT_ID before or it is disabled.
                then goto following link and enable the access
                https://console.developers.google.com/apis/api/youtube.googleapis.com/overview?project=YOUR_PROJECT_ID*/

                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(Subscription subscription) {
            super.onPostExecute(subscription);
            if (subscription != null) {
                view.onSubscribetionSuccess(subscription.getSnippet().getTitle());
            } else {
                view.onSubscribetionFail();
            }
        }
    }
}
