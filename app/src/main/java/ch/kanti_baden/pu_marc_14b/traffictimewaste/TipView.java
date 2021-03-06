package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;

public class TipView extends LinearLayout {
    // Constructors inherited from LinearLayout
    public TipView(Context context) {
        super(context);
    }
    public TipView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public TipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @RequiresApi(21)
    public TipView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
    }

    private static final String BUTTON_TRIGGER = "[SPOILER]";
    private static final String IMAGE_TRIGGER = "[IMG";
    public static final String TITLE_TRIGGER = "[TITLE:";
    private static final ViewGroup.LayoutParams LAYOUT_PARAMS;

    static {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 50);
        LAYOUT_PARAMS = layoutParams;
    }

    void setContent(String text) {
        text = removeTitle(text);

        TextView textView = new TextView(getContext());
        textView.setLayoutParams(LAYOUT_PARAMS);
        textView.setText(text);
        addView(textView);

        insertImages(text);
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) instanceof TextView)
                insertButtons(((TextView) getChildAt(i)).getText().toString(), i);
        }

        boolean foundButton = false;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setVisibility(foundButton ? View.GONE : View.VISIBLE);

            if (!foundButton)
                foundButton = getChildAt(i) instanceof Button;
        }
    }

    private static String removeTitle(String text) {
        if (text.contains(TITLE_TRIGGER)) {
            int triggerIndex = text.indexOf(TITLE_TRIGGER);
            text = text.substring(0, triggerIndex)
                    + text.substring(text.indexOf("]", triggerIndex) + 1);
        }
        return text;
    }

    private void insertImages(String content) { // [BILD http://...]
        if (!content.contains(IMAGE_TRIGGER))
            return;

        int startIndex = content.indexOf(IMAGE_TRIGGER); // just before trigger
        int endIndex = content.indexOf(']', startIndex); // just before ]

        // Extract link URL
        String url = content.substring(startIndex + IMAGE_TRIGGER.length(), endIndex).replaceAll(" ", "");

        // Create and display ImageView with loading screen
        final ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(LAYOUT_PARAMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_loading, getContext().getTheme()));
        } else {
            //noinspection deprecation
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_loading));
        }

        // Create and start AsyncTask to load the referenced image
        AsyncTask<String, Void, Bitmap> imageLoadingTask = new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                try {
                    URL url = new URL(params[0]);
                    return BitmapFactory.decodeStream(url.openConnection().getInputStream());
                } catch (IOException e) {
                    Log.e("TrafficTimeWaste", "Unable to load image", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap !=  null)
                    imageView.setImageBitmap(bitmap);
            }
        };
        imageLoadingTask.execute(url);

        String precedingText = content.substring(0, startIndex);
        String followingText = content.substring(endIndex + 1);

        // Update children
        if (getChildCount() > 0 && getChildAt(getChildCount() - 1) instanceof TextView) {
            TextView textView = (TextView) getChildAt(getChildCount() - 1);
            textView.setText(precedingText);
        } else {
            TextView textView = new TextView(getContext());
            textView.setLayoutParams(LAYOUT_PARAMS);
            textView.setText(precedingText);
            addView(textView);
        }

        addView(imageView);

        TextView textView = new TextView(getContext());
        textView.setLayoutParams(LAYOUT_PARAMS);
        textView.setText(followingText);
        addView(textView);

        // Recursively call insertImages
        insertImages(followingText);
    }

    private void insertButtons(String content, int childPosition) { // [SPOILER]
        if (!content.contains(BUTTON_TRIGGER))
            return;

        int startIndex = content.indexOf(BUTTON_TRIGGER); // just before [

        String precedingText = content.substring(0, startIndex);
        String followingText = content.substring(startIndex+ BUTTON_TRIGGER.length());

        // Create and display Button with text
        Button button = new Button(getContext());
        button.setLayoutParams(LAYOUT_PARAMS);
        button.setText(R.string.spoiler_text);

        // Update children
        TextView textView = (TextView) getChildAt(childPosition);
        textView.setText(precedingText);

        final int buttonIndex = childPosition + 1;
        addView(button, buttonIndex);

        if (getChildCount() > childPosition + 1 && getChildAt(childPosition + 2) instanceof TextView) {
            TextView textView2 = (TextView) getChildAt(childPosition + 2);
            String newText = followingText + textView2.getText();
            textView2.setText(newText);
        } else {
            TextView textView2 = new TextView(getContext());
            textView2.setLayoutParams(LAYOUT_PARAMS);
            textView2.setText(followingText);
            addView(textView2, childPosition + 2);
        }

        // Setup OnClickListener
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide button
                getChildAt(buttonIndex).setVisibility(View.GONE);

                // Show everything until next button
                for (int i = buttonIndex+1; i < getChildCount(); i++) {
                    View view = getChildAt(i);
                    view.setVisibility(View.VISIBLE);

                    if (view instanceof Button)
                        break;
                }
            }
        });

        // Recursively call insertButtons
        insertButtons(followingText, childPosition + 2);
    }
}
