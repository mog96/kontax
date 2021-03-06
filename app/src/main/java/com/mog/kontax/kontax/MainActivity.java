package com.mog.kontax.kontax;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ContactListAdapter.ContactListItemClickListener {

    RecyclerView mRecyclerView;
    ContactListAdapter mContactListAdapter;

    // TextView mTextView;

    private TextView mErrorMessageDisplay;

    private ProgressBar mLoadingIndicator;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // mTextView = (TextView) findViewById(R.id.tv_json_display);

        mErrorMessageDisplay = (TextView) findViewById(R.id.errorMessageDisplayTextView);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.loadingIndicatorProgressBar);

        mRecyclerView = (RecyclerView) findViewById(R.id.contactListRecyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true); // Tells RecyclerView that all list items have same size

        mContactListAdapter = new ContactListAdapter(this);

        mRecyclerView.setAdapter(mContactListAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        loadContacts();
    }

    public void loadContacts() {
        ParseQuery<Contact> query = ParseQuery.getQuery(Contact.class);
        query.whereEqualTo("ownerId", ParseUser.getCurrentUser().getObjectId());

        mLoadingIndicator.setVisibility(View.VISIBLE);

        query.findInBackground(new FindCallback<Contact>() {
            public void done(List<Contact> itemList, ParseException exception) {
                mLoadingIndicator.setVisibility(View.INVISIBLE);

                if (exception == null) {
                    showRecyclerView();

                    displayContacts(itemList);

                    // Access the array of results here
                    Toast.makeText(MainActivity.this, "Phew, loaded em.", Toast.LENGTH_SHORT).show();
                } else {
                    showErrorMessage();

                    Log.d("item", "Error: " + exception.getMessage());
                }
            }
        });
    }

    public void displayContacts(List<Contact> contacts) {
        Contact[] contactArray = contacts.toArray(new Contact[contacts.size()]);
        mContactListAdapter.setContacts(contactArray);
    }

    public void showRecyclerView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    public void showErrorMessage() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    public void presentNewContactActivity(View view) {
        Context context = MainActivity.this;
        Class destinationActivity = NewContactActivity.class;
        Intent intent = new Intent(context, destinationActivity);
        startActivity(intent);
    }

    public void presentContactDetailActivity(Contact contact) {
        Log.d("MainActivity", "PRESENTING DETAIL FOR " + contact.getName());

        Context context = MainActivity.this;
        Class destinationActivity = ContactDetailActivity.class;

        Bundle bundle = new Bundle();
        bundle.putParcelable("BANANA", contact);

        Intent intent = new Intent(context, destinationActivity);
        intent.putExtras(bundle);

        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            loadContacts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(int clickedItemIndex, Contact contact) {
        /*
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), contact.getName(), Toast.LENGTH_LONG);
        mToast.show();
        */
        presentContactDetailActivity(contact);
    }
}
