package com.tonyw.sampleapps.palettecolorextraction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.GridView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    private static final int REQUEST_CODE_ACTION_ADD_FROM_STORAGE = 0;
    private static final int REQUEST_CODE_ACTION_ADD_FROM_CAMERA = 1;

    private ArrayList<Bitmap> mBitmaps;
    private GridView mGridView;
    private CardAdapter mCardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBitmaps = new ArrayList<>();
        mGridView = (GridView) findViewById(R.id.color_background);
        mCardAdapter = new CardAdapter(this, mBitmaps, mGridView);
        mGridView.setAdapter(mCardAdapter);

        // Make cards dismissible.
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        mGridView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(AbsListView view, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    mCardAdapter.remove(position);
                                }
                                mCardAdapter.notifyDataSetChanged();
                            }
                        });
        mGridView.setOnTouchListener(touchListener);
        // Set this scroll listener to ensure that we don't look for swipes during scrolling.
        mGridView.setOnScrollListener(touchListener.makeScrollListener());

        if (savedInstanceState == null) {
            try {
                addCards();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mCardAdapter.notifyDataSetChanged();
    }

    /**
     * Adds cards with the default images stored in assets.
     */
    private void addCards() throws IOException {
        AssetManager assetManager = getAssets();
        for (String assetName : assetManager.list("sample_images")) {
            addCard(getSampledBitmap(new AssetStreamProvider("sample_images/" + assetName)));
        }

        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Backdrops/Wallpapers");
        for (File f : file.listFiles()) {
            addCard(getSampledBitmap(new UriStreamProvider(Uri.fromFile(f))));
        }
    }

    /**
     * Adds the provided bitmap to a list, and repopulates the main GridView with the new card.
     */
    private void addCard(Bitmap bitmap) {
        if (null != bitmap) {
            mBitmaps.add(bitmap);
            mCardAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_add_from_camera == item.getItemId()) {
            // Start Intent to retrieve an image (see OnActivityResult).
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, REQUEST_CODE_ACTION_ADD_FROM_CAMERA);
            return true;
        } else if (R.id.action_add_from_storage == item.getItemId()) {
            // Start Intent to retrieve an image (see OnActivityResult).
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_ACTION_ADD_FROM_STORAGE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = null;
        if (Activity.RESULT_OK == resultCode) {
            if (REQUEST_CODE_ACTION_ADD_FROM_STORAGE == requestCode) {
                bitmap = getSampledBitmap(new UriStreamProvider(data.getData()));
            } else if (REQUEST_CODE_ACTION_ADD_FROM_CAMERA == requestCode) {
                Bundle extras = data.getExtras();
                bitmap = (Bitmap) extras.get("data"); // Just a thumbnail, but works okay for this.
            }
        }
        if (bitmap != null) {
            addCard(bitmap);
            mGridView.smoothScrollToPosition(mBitmaps.size() - 1);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private Bitmap getSampledBitmap(StreamProvider provider) {
        InputStream is = null;
        try {
            is = provider.open(this);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            is = provider.open(this);
            options.inJustDecodeBounds = false;
            options.inSampleSize = calculateInSampleSize(options, 512, 512);
            return BitmapFactory.decodeStream(is, null, options);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    interface StreamProvider {
        InputStream open(Context context) throws IOException;
    }

    static class UriStreamProvider implements StreamProvider {
        Uri uri;
        UriStreamProvider(Uri uri) {
            this.uri = uri;
        }

        @Override
        public InputStream open(Context context) throws FileNotFoundException {
            return context.getContentResolver().openInputStream(uri);
        }
    }
    static class AssetStreamProvider implements StreamProvider {
        String assetPath;
        AssetStreamProvider(String assetPath) {
            this.assetPath = assetPath;
        }

        @Override
        public InputStream open(Context context) throws IOException {
            return context.getAssets().open(assetPath);
        }
    }
}
