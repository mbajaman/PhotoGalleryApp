package com.example.photogalleryapp.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.photogalleryapp.GpsTracker;
import com.example.photogalleryapp.R;
import com.example.photogalleryapp.SearchActivity;
import com.example.photogalleryapp.presenters.MainPresenter;
import com.example.photogalleryapp.presenters.MainPresenterImpl;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements MainView {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int SEARCH_ACTIVITY_REQUEST_CODE = 2;
    String mCurrentPhotoPath;
    String cityName = null;
    private ArrayList<String> photos = null;
    private int index = 0;
    private TextView tvCity;
    private MainPresenter presenter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        presenter = new MainPresenterImpl(this); //Adding presenter
        presenter.bindView(this);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                        }
                    }
                });

        tvCity = findViewById(R.id.text_city);
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        presenter.findPhotos(new Date(Long.MIN_VALUE), new Date(), "", "");
    }

    public void getLocation(View view){
        GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
        if(gpsTracker.canGetLocation()){
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;

            try {
                addresses = gcd.getFromLocation(gpsTracker.getLatitude(),
                        gpsTracker.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
                    cityName = addresses.get(0).getLocality();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            tvCity.setText(cityName);
        }else{
            gpsTracker.showSettingsAlert();
        }
    } // Getting Data

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEARCH_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                DateFormat format = new SimpleDateFormat("yyyy‐MM‐dd HH:mm:ss", Locale.CANADA);
                Date startTimestamp , endTimestamp;
                try {
                    String from = data.getStringExtra("STARTTIMESTAMP");
                    String to = data.getStringExtra("ENDTIMESTAMP");
                    startTimestamp = format.parse(from);
                    endTimestamp = format.parse(to);
                } catch (Exception ex) {
                    startTimestamp = null;
                    endTimestamp = null;
                }
                String keywords = data.getStringExtra("KEYWORDS");
                String locate = data.getStringExtra("LOCATE");
                index = 0;
                photos = findPhotos(startTimestamp, endTimestamp, keywords, locate);
                if (photos.size() == 0) {
                    displayPhoto(null);
                } else {
                    displayPhoto(photos.get(index));
                }
            }
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ImageView mImageView = findViewById(R.id.ivGallery);
            mImageView.setImageBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath));
            photos = findPhotos(new Date(Long.MIN_VALUE), new Date(), "", "");
        }
    }

    private ArrayList<String> findPhotos(Date startTimestamp, Date endTimestamp, String keywords, String locate){
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/Android/data/com.example.photogalleryapp/files/Pictures");
        ArrayList<String> photos = new ArrayList<>();
        File[] fList = file.listFiles();
        if(fList != null){
            for (File f : fList) {
                if (((startTimestamp == null && endTimestamp == null) || (f.lastModified() >= startTimestamp.getTime()
                        && f.lastModified() <= endTimestamp.getTime())) && (keywords.equals("") || f.getPath().contains(keywords))  && (locate.equals("") || f.getPath().contains(locate))) {
                    photos.add(f.getPath());
                }
            }
        }
        return photos;
    }

    public void takePhoto(View v){
        presenter.takePhoto();
    }

    @Override
    public void displayPhoto(String path) {
        ImageView iv = findViewById(R.id.ivGallery);
        TextView tv = findViewById(R.id.tvTimestamp);
        EditText et = findViewById(R.id.etCaption);

        if(path == null || path.equals("")){
            iv.setImageResource(R.mipmap.ic_launcher);
            et.setText("");
            tv.setText("");
        } else {
            iv.setImageBitmap(BitmapFactory.decodeFile(path));
            String[] attr = path.split("_");
            et.setText(attr[1]);
            tv.setText("Date: " + attr[2] + "  Time: " + attr[3] + "\nCity: " + attr[4]);
        }
    }

    private void updatePhoto(String path, String caption) {
        String[] attr = path.split("_");
        if (attr.length >= 3) {
            File to = new File(attr[0] + "_" + caption + "_" + attr[2] + "_" + attr[3] + "_" + attr[4] + "_" + ".jpeg");
            File from = new File(path);
            from.renameTo(to);
        }
    } // Getting Data


    public void scrollPhotos(View v) {
        //updatePhoto(photos.get(index), ((EditText) findViewById(R.id.etCaption)).getText().toString());
        switch (v.getId()) {
            case R.id.btnPrev:
                presenter.handleNavigationInput("ScrollLeft", "caption");
                break;
            case R.id.btnNext:
                presenter.handleNavigationInput("ScrollRight", "caption");
                break;
            default:
                break;
        }
    }

    public void searchButton(View v){
        Intent intent = new Intent(this, SearchActivity.class);
        startActivityForResult(intent, SEARCH_ACTIVITY_REQUEST_CODE);
    } // Getting Data

    // Share to social media
    public void shareButton(View v) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        File photoFile = new File(photos.get(index));
        Uri photoURI = FileProvider.getUriForFile(this, "com.example.photogalleryapp.fileprovider", photoFile);
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
        shareIntent.setType("image/jpeg");

        startActivity(Intent.createChooser(shareIntent, "Share to"));
    } // Getting Data
}