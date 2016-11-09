package ch.kanti_baden.pu_marc_14b.traffictimewaste;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

public class TipCreateActivity extends AppCompatActivity {

    private EditText postContent;
    private EditTagView editTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_create);

        postContent = (EditText) findViewById(R.id.postCreateContent);
        editTag = (EditTagView) findViewById(R.id.tagEditText);
        editTag.init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_tip_create, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_submit:
                String content = DatabaseLink.encodeSpecial(postContent.getText().toString());
                String tags = editTag.getInput();

                final ProgressDialog progressDialog = ProgressDialog.show(this,
                        getResources().getString(R.string.progress_submitting),
                        getResources().getString(R.string.progress_please_wait),
                        true, false);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();

                DatabaseLink.instance.createPost(new DatabaseLink.DatabaseListener() {
                    @Override
                    void onGetResponse(String json) {
                        progressDialog.dismiss();

                        boolean success;
                        String message;

                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            success = jsonObject.getInt(DatabaseLink.JSON_SUCCESS) == 1;
                            message = jsonObject.getString(DatabaseLink.JSON_MESSAGE);
                        } catch (JSONException e) {
                            success = false;
                            message = e.getMessage();
                        }

                        DialogInterface.OnClickListener onClickListener;
                        if (success) {
                            onClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setResult(PostListActivity.ACTIVITY_SUCCESS);
                                    finish();
                                }
                            };
                        } else {
                            onClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            };
                        }

                        AlertDialog alertDialog = new AlertDialog.Builder(TipCreateActivity.this)
                                .setMessage(message)
                                .setPositiveButton(R.string.okay, onClickListener)
                                .create();
                        alertDialog.show();
                    }

                    @Override
                    void onError(String errorMsg) {
                        onGetResponse("{\"success\"=0, \"message\"='" + errorMsg + "'}");
                    }
                }, content, tags);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
