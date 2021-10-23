package com.example.photogalleryapp.presenters;

import static android.app.Activity.RESULT_OK;
import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.example.photogalleryapp.SearchActivity;
import com.example.photogalleryapp.model.Photo;
import com.example.photogalleryapp.model.PhotoRepository;
import com.example.photogalleryapp.views.MainView;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainPresenterImpl implements MainPresenter {

    private MainView view;
    private final Activity context;
    Bundle bundle;
    private ArrayList<Photo> photos = null;
    String cityName = null;
    String mCurrentPhotoPath;
    private PhotoRepository repository;
    private int index = 0;

    static final int SEARCH_ACTIVITY_REQUEST_CODE = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    public void bindView(MainView view) {
        this.view = view;
    }

    @Override
    public void dropView() {
        this.view = null;
    }

    public MainPresenterImpl(Activity context) {
        this.context = context;
        repository = new PhotoRepository(context);
    }

    public void onReturn(int requestCode, int resultCode, Intent data){
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
                findPhotos(startTimestamp, endTimestamp, keywords, locate);
                if (repository.getPhotos().size() == 0) {
                    view.displayPhoto(null);
                } else {
                    view.displayPhoto(repository.getPhotos().get(index).getPhotoPath());
                }
            }
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //ImageView mImageView = findViewById(R.id.ivGallery);
            //mImageView.setImageBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath));
            findPhotos(new Date(Long.MIN_VALUE), new Date(), "", "");
        }
    }

    public void findPhotos(Date startTimestamp, Date endTimestamp, String keywords, String locate){
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/Android/data/com.example.photogalleryapp/files/Pictures");
        ArrayList<Photo> photos = new ArrayList<>();
        File[] fList = file.listFiles();
        if(fList != null){
            for (File f : fList) {
                if (((startTimestamp == null && endTimestamp == null) || (f.lastModified() >= startTimestamp.getTime()
                        && f.lastModified() <= endTimestamp.getTime())) && (keywords.equals("") || f.getPath().contains(keywords))  && (locate.equals("") || f.getPath().contains(locate))) {
                    repository.PhotoCreate(f.getPath());
                }
            }
            for (Photo p : repository.getPhotos()) {
                view.displayPhoto(p.getPhotoPath());
            }
        }
    }

    public void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                //Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(context, "com.example.photogalleryapp.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(context, takePictureIntent, REQUEST_IMAGE_CAPTURE, bundle);
            }
        }
        view.displayPhoto(repository.lastPhoto());
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CANADA).format(new Date());
        String imageFileName = "_caption_" + timeStamp + "_" + cityName + "_";

        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        repository.PhotoCreate(image.getAbsolutePath());
        //System.out.println(repository.getPhotos());
        return image;
    }

    public void updatePhoto(String caption) {
        String path = repository.getPhotos().get(index).getPhotoPath();
        String[] attr = path.split("_");
        if (attr.length >= 3) {
            File to = new File(attr[0] + "_" + caption + "_" + attr[2] + "_" + attr[3] + "_" + attr[4] + "_" + ".jpeg");
            File from = new File(path);
            from.renameTo(to);
        }
    }

    public void handleNavigationInput(String navigationAction, String caption){

        switch (navigationAction){
            case "ScrollLeft":
                if(index > 0){
                    index--;
                    view.displayPhoto(repository.getPhotos().get(index).getPhotoPath());
                }
                break;
            case "ScrollRight":
                if(index < (repository.getPhotos().size() - 1)) {
                    index++;
                    view.displayPhoto(repository.getPhotos().get(index).getPhotoPath());
                }
                break;
            default:
                break;
        }

    }

    public Intent sharePhoto() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        File photoFile = new File(repository.getPhotos().get(index).getPhotoPath());
        Uri photoURI = FileProvider.getUriForFile(context, "com.example.photogalleryapp.fileprovider", photoFile);
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
        shareIntent.setType("image/jpeg");
        return shareIntent;
    }

    public Intent search() {
        Intent intent = new Intent(context, SearchActivity.class);
        return intent;
    }
}
