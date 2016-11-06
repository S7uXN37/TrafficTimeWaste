package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class EditTagView extends EditText {
    // Constructors inherited from View
    public EditTagView(Context context) {
        super(context);
    }
    public EditTagView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public EditTagView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @RequiresApi(21)
    public EditTagView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private final SpannableStringBuilder sb = new SpannableStringBuilder();

    String getInput() {
        update(getText().toString());
        return sb.toString();
    }

    void init() {
        TextWatcher textWatcher = new TextWatcher() {
            boolean mustUpdate = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mustUpdate = (count == 1 && (s.charAt(start) == ',' || s.charAt(start) == ' '));
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mustUpdate)
                    update(s.toString());
                mustUpdate = false;
            }
        };

        addTextChangedListener(textWatcher);
    }

    private void update(String text) {
        if (text.length() <= 0)
            return;

        sb.clearSpans();
        sb.clear();

        for (String tag : text.split(",")) {
            tag = removeSpaces(tag);
            if (tag.length() > 0)
                addSpannedTag(tag);
        }

        // Update input view
        setText(sb);
    }

    private static String removeSpaces(String tag) {
        String newTag = tag;

        // Remove leading & trailing spaces
        while (newTag.length() > 0 && newTag.charAt(0) == ' ')
            newTag = newTag.substring(1);
        while (newTag.length() > 0 && newTag.charAt(newTag.length() - 1) == ' ')
            newTag = newTag.substring(0, newTag.length() - 1);

        return newTag;
    }

    private void addSpannedTag(String tag) {
        // Create background
        TextView tv = createReplacementView(tag);
        BitmapDrawable bd = convertViewToDrawable(tv);
        bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());

        // Format tag with background
        sb.append(tag);
        sb.append(",");
        sb.setSpan(new ImageSpan(bd), sb.length() - (tag.length() + 1), sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private TextView createReplacementView(String text){
        // Create TextView dynamically
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(20);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tv.setTextColor(getResources().getColor(R.color.colorPrimary, getContext().getTheme()));
        } else {
            //noinspection deprecation
            tv.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
        tv.setBackgroundResource(R.drawable.oval);
        return tv;
    }

    private BitmapDrawable convertViewToDrawable(View view) {
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());

        view.draw(c);
        view.setDrawingCacheEnabled(true);

        Bitmap cacheBmp = view.getDrawingCache();
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);

        view.destroyDrawingCache();
        return new BitmapDrawable(getResources(), viewBmp);
    }
}