package com.mog.kontax.kontax;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mog.kontax.kontax.databinding.ActivityNewContactBinding;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewContactActivity extends AppCompatActivity {

    static final int RESULT_IMAGE_CAPTURE = 1;
    static final int RESULT_PICK_IMAGE = 2;

    ActivityNewContactBinding mBinding;

    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_contact);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_new_contact);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.new_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveContact();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveContact() {
        Contact newContact = new Contact();

        // Sets owner to current user.
        newContact.setOwnerId();

        String name = mBinding.nameEditText.getText().toString();
        newContact.setName(name);

        String phone = mBinding.phoneEditText.getText().toString();
        newContact.setPhone(phone);

        String email = mBinding.emailEditText.getText().toString();
        newContact.setEmail(email);

        newContact.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException exception) {
                if (exception == null) {
                    Toast.makeText(getApplicationContext(), "Contact saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("new contact", "Error: " + exception.getMessage());
                }
            }
        });
    }

    public void presentPhotoSelectionOptions(View view) {
        // dispatchImageCaptureIntent();
        dispatchPickImageIntent();
    }

    // Allows user to choose image source.
    private void dispatchPickImageIntent() {
        Intent intent = new Intent();
        // Show only images. No videos or anything else.
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available).
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_PICK_IMAGE);

        /*
        Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, RESULT_PICK_IMAGE);
        */
    }

    // Opens camera app and passes it a filename for the captured photo.
    private void dispatchImageCaptureIntent() {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (imageCaptureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException exception) {
                // Error occurred while creating the File
                exception.printStackTrace();
                Log.d("image capture", "Error: " + exception.getMessage());
            }

            // Continue only if the File was successfully created
            if (photoFile == null) {
                Toast.makeText(getApplicationContext(), "An error occurred while saving your photo :[", Toast.LENGTH_LONG).show();
            } else {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.mog.kontax.fileprovider",
                        photoFile);
                imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(imageCaptureIntent, RESULT_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("new contact", "ENTERED ACTIVITY RESULT");

        if (requestCode == RESULT_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d("new contact", "HANDLE REQUEST_IMAGE_CAPTURE");

            // Photo has been written to the file name specified in mCurrentPhotoPath.
            Bitmap imageBitmap = getImageViewSizedBitmap(mCurrentPhotoPath);

            // Photo is newly taken and has not been fully processed,
            // so we must rotate it ourselves.
            Bitmap rotatedBitmap = getRotatedImageBitmap(imageBitmap);

            mBinding.photoImageView.setImageBitmap(rotatedBitmap);

        } else if (requestCode == RESULT_PICK_IMAGE && resultCode == RESULT_OK) {
            Log.d("new contact", "HANDLE REQUEST_PICK_IMAGE");

            Uri selectedImage = data.getData();
            String selectedImagePath = getRealPathFromURI(getApplicationContext(), selectedImage);
            Bitmap imageBitmap = getImageViewSizedBitmap(selectedImagePath);
            mBinding.photoImageView.setImageBitmap(imageBitmap);
        }
    }

    // MARK: - ImageView Set Image Helpers

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
        String imageFileName = timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents.
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap getImageViewSizedBitmap(String imageFilePath) {
        // Get the dimensions of the View
        int targetW = mBinding.photoImageView.getWidth();
        int targetH = mBinding.photoImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFilePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Log.d("new contact", "SIZED BITMAP ");

        return BitmapFactory.decodeFile(imageFilePath, bmOptions);
    }

    private Bitmap getRotatedImageBitmap(Bitmap imageBitmap) {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(mCurrentPhotoPath);
        } catch (IOException exception) {
            exception.printStackTrace();
            Log.d("rotate image", "Error: " + exception.getMessage());
        }

        if (exifInterface == null) {
            retrievePhotoError();
            return null;

        } else {
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            Bitmap rotatedBitmap = null;
            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(imageBitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(imageBitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(imageBitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:

                default:
                    break;
            }

            Log.d("new contact", "ROTATED BITMAP");

            return rotatedBitmap;
        }
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void retrievePhotoError() {
        Toast.makeText(getApplicationContext(), "An error occurred while retreiving your photo :[", Toast.LENGTH_LONG).show();
    }
}
