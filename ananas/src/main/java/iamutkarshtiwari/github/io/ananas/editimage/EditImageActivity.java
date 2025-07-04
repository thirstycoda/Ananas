package iamutkarshtiwari.github.io.ananas.editimage;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.theartofdev.edmodo.cropper.CropImageView;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import java.util.HashMap;

import iamutkarshtiwari.github.io.ananas.BaseActivity;
import iamutkarshtiwari.github.io.ananas.R;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.AddTextFragment;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.BeautyFragment;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.BrightnessFragment;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.FilterListFragment;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.MainMenuFragment;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.RotateFragment;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.SaturationFragment;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.crop.AspectRatio;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.crop.CropFragment;
import iamutkarshtiwari.github.io.ananas.editimage.fragment.paint.PaintFragment;
import iamutkarshtiwari.github.io.ananas.editimage.interfaces.OnLoadingDialogListener;
import iamutkarshtiwari.github.io.ananas.editimage.utils.BitmapUtils;
import iamutkarshtiwari.github.io.ananas.editimage.utils.PermissionUtils;
import iamutkarshtiwari.github.io.ananas.editimage.view.BrightnessView;
import iamutkarshtiwari.github.io.ananas.editimage.view.CustomViewPager;
import iamutkarshtiwari.github.io.ananas.editimage.view.RotateImageView;
import iamutkarshtiwari.github.io.ananas.editimage.view.SaturationView;
import iamutkarshtiwari.github.io.ananas.editimage.view.imagezoom.ImageViewTouch;
import iamutkarshtiwari.github.io.ananas.editimage.view.imagezoom.ImageViewTouchBase;
import iamutkarshtiwari.github.io.ananas.editimage.widget.RedoUndoController;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class EditImageActivity extends BaseActivity implements OnLoadingDialogListener {
    public static final String IS_IMAGE_EDITED = "is_image_edited";
    public static final int MODE_NONE = 0;
    public static final int MODE_FILTER = 1;
    public static final int MODE_CROP = 2;
    public static final int MODE_ROTATE = 3;
    public static final int MODE_TEXT = 4;
    public static final int MODE_PAINT = 5;
    public static final int MODE_BEAUTY = 6;
    public static final int MODE_BRIGHTNESS = 7;
    public static final int MODE_SATURATION = 8;
    public static HashMap<String, Typeface> fonts;
    private static final int PERMISSIONS_REQUEST_CODE = 110;

    private final String[] requiredPermissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public Uri sourceUri;
    public String sourceFilePath;
    public String outputFilePath;
    public String editorTitle;
    public CropImageView cropPanel;
    public ImageViewTouch mainImage;
    public int mode = MODE_NONE;
    protected boolean isBeenSaved = false;
    protected boolean isPortraitForced = false;
    protected boolean isSupportActionBarEnabled = false;
    public ViewFlipper bannerFlipper;
    public BrightnessView brightnessView;
    public SaturationView saturationView;
    public RotateImageView rotatePanel;
    public CustomViewPager bottomGallery;
    public FilterListFragment filterListFragment;
    public CropFragment cropFragment;
    public RotateFragment rotateFragment;
    public AddTextFragment addTextFragment;
    public PaintFragment paintFragment;
    public BeautyFragment beautyFragment;
    public BrightnessFragment brightnessFragment;
    public SaturationFragment saturationFragment;
    protected int numberOfOperations = 0;
    private int imageWidth, imageHeight;
    private Bitmap mainBitmap;
    private Dialog loadingDialog;
    private MainMenuFragment mainMenuFragment;
    private RedoUndoController redoUndoController;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private int backResId;

    private List<AspectRatio> aspectRatios;
    private int aspectRatioMinX;
    private int aspectRatioMinY;
    private float aspectRatioMin;
    private int aspectRatioMaxX;
    private int aspectRatioMaxY;
    private float aspectRatioMax;

    public static void start(ActivityResultLauncher<Intent> launcher, Intent intent, Context context) {
        String sourcePath = intent.getStringExtra(ImageEditorIntentBuilder.SOURCE_PATH);
        String sourceUriStr = intent.getStringExtra(ImageEditorIntentBuilder.SOURCE_URI);

        if (TextUtils.isEmpty(sourcePath) && TextUtils.isEmpty(sourceUriStr)) {
            Toast.makeText(context, R.string.iamutkarshtiwari_github_io_ananas_not_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        launcher.launch(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);
        getData();
        initView();
    }

    @Override
    public void showLoadingDialog() {
        loadingDialog.show();
    }

    @Override
    public void dismissLoadingDialog() {
        loadingDialog.dismiss();
    }

    private void getData() {
        isPortraitForced = getIntent().getBooleanExtra(ImageEditorIntentBuilder.FORCE_PORTRAIT, false);
        isSupportActionBarEnabled  = getIntent().getBooleanExtra(ImageEditorIntentBuilder.SUPPORT_ACTION_BAR_VISIBILITY, false);

        String sourceUriStr = getIntent().getStringExtra(ImageEditorIntentBuilder.SOURCE_URI);
        if (!TextUtils.isEmpty(sourceUriStr)) {
            sourceUri = Uri.parse(sourceUriStr);
        }

        sourceFilePath = getIntent().getStringExtra(ImageEditorIntentBuilder.SOURCE_PATH);
        outputFilePath = getIntent().getStringExtra(ImageEditorIntentBuilder.OUTPUT_PATH);
        editorTitle = getIntent().getStringExtra(ImageEditorIntentBuilder.EDITOR_TITLE);
        backResId = getIntent().getIntExtra(ImageEditorIntentBuilder.BACK_RESOURCE_ID, 0);

        aspectRatios = (List<AspectRatio>) getIntent().getSerializableExtra(ImageEditorIntentBuilder.ASPECT_RATIOS);
        aspectRatioMinX = getIntent().getIntExtra(ImageEditorIntentBuilder.ASPECT_RATIO_MIN_X, 0);
        aspectRatioMinY = getIntent().getIntExtra(ImageEditorIntentBuilder.ASPECT_RATIO_MIN_Y, 0);
        aspectRatioMaxX = getIntent().getIntExtra(ImageEditorIntentBuilder.ASPECT_RATIO_MAX_X, 0);
        aspectRatioMaxY = getIntent().getIntExtra(ImageEditorIntentBuilder.ASPECT_RATIO_MAX_Y, 0);

        if (aspectRatioMinX > 0 && aspectRatioMinY > 0) {
            aspectRatioMin = (float) aspectRatioMinX/aspectRatioMinY;
        }

        if (aspectRatioMaxX > 0 && aspectRatioMaxY > 0) {
            aspectRatioMax = (float) aspectRatioMaxX/aspectRatioMaxY;
        }
    }

    private void initView() {
        TextView titleView = findViewById(R.id.title);
        if (editorTitle != null) {
            titleView.setText(editorTitle);
        }
        loadingDialog = BaseActivity.getLoadingDialog(this, R.string.iamutkarshtiwari_github_io_ananas_loading,
                false);

        if (getSupportActionBar() != null) {
            if (isSupportActionBarEnabled) {
                getSupportActionBar().show();
            } else {
                getSupportActionBar().hide();
            }
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        imageWidth = metrics.widthPixels / 2;
        imageHeight = metrics.heightPixels / 2;

        bannerFlipper = findViewById(R.id.banner_flipper);
        bannerFlipper.setInAnimation(this, R.anim.in_bottom_to_top);
        bannerFlipper.setOutAnimation(this, R.anim.out_bottom_to_top);
        View applyBtn = findViewById(R.id.apply);
        applyBtn.setOnClickListener(new ApplyBtnClick());
        View saveBtn = findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(new SaveBtnClick());

        mainImage = findViewById(R.id.main_image);

        ImageView backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> onBackPressed());

        if (backResId > 0) {
            backBtn.setImageResource(backResId);
        }

        cropPanel = findViewById(R.id.crop_panel);
        rotatePanel = findViewById(R.id.rotate_panel);
        brightnessView = findViewById(R.id.brightness_panel);
        saturationView = findViewById(R.id.contrast_panel);
        bottomGallery = findViewById(R.id.bottom_gallery);

        mainMenuFragment = MainMenuFragment.newInstance();
        mainMenuFragment.setArguments(getIntent().getExtras());

        BottomGalleryAdapter bottomGalleryAdapter = new BottomGalleryAdapter(
                this.getSupportFragmentManager());
        filterListFragment = FilterListFragment.newInstance();

        cropFragment = CropFragment.newInstance();
        cropFragment.setAspectRatios(aspectRatios);
        cropFragment.setMinimumAspectRatio(aspectRatioMinX, aspectRatioMinY);
        cropFragment.setMaximumAspectRatio(aspectRatioMaxX, aspectRatioMaxY);

        rotateFragment = RotateFragment.newInstance();
        paintFragment = PaintFragment.newInstance();
        beautyFragment = BeautyFragment.newInstance();
        brightnessFragment = BrightnessFragment.newInstance();
        saturationFragment = SaturationFragment.newInstance();
        addTextFragment = AddTextFragment.newInstance(fonts);

        bottomGallery.setAdapter(bottomGalleryAdapter);

        mainImage.setFlingListener((e1, e2, velocityX, velocityY) -> {
            if (velocityY > 1) {
                closeInputMethod();
            }
        });

        redoUndoController = new RedoUndoController(this, findViewById(R.id.redo_undo_panel));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !PermissionUtils.hasPermissions(this, requiredPermissions)) {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE);
        }

        if (!TextUtils.isEmpty(sourceFilePath)) {
            loadImageFromFile(sourceFilePath);
        } else {
            URL url = null;

            if (sourceUri.getScheme().startsWith("http")) {
                try {
                    url = new URL(sourceUri.toString());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

            if (url != null) {
                loadImageFromUrl(url);
            } else {
                loadImageFromUri(sourceUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String permissions[], @NotNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Lock orientation for this activity
        if (isPortraitForced) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setLockScreenOrientation(true);
        }
    }

    private void closeInputMethod() {
        if (addTextFragment.isAdded()) {
            addTextFragment.hideInput();
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }

    @Override
    public void onBackPressed() {
        switch (mode) {
            case MODE_FILTER:
                filterListFragment.backToMain();
                break;
            case MODE_CROP:
                cropFragment.backToMain();
                break;
            case MODE_ROTATE:
                rotateFragment.backToMain();
                break;
            case MODE_TEXT:
                addTextFragment.backToMain();
                break;
            case MODE_PAINT:
                paintFragment.backToMain();
                break;
            case MODE_BEAUTY:
                beautyFragment.backToMain();
                break;
            case MODE_BRIGHTNESS:
                brightnessFragment.backToMain();
                break;
            case MODE_SATURATION:
                saturationFragment.backToMain();
                break;
            default:
                if (canAutoExit()) {
                    setResult(RESULT_CANCELED);
                    finish();
                } else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setMessage(R.string.iamutkarshtiwari_github_io_ananas_exit_without_save)
                            .setCancelable(false)
                            .setPositiveButton(R.string.iamutkarshtiwari_github_io_ananas_confirm, (dialog, id) -> {
                                setResult(RESULT_CANCELED);
                                finish();
                            })
                            .setNegativeButton(R.string.iamutkarshtiwari_github_io_ananas_cancel, (dialog, id) -> dialog.cancel());

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                break;
        }
    }

    public void changeMainBitmap(Bitmap newBit, boolean needPushUndoStack) {
        if (newBit == null)
            return;

        if (mainBitmap == null || mainBitmap != newBit) {
            if (needPushUndoStack) {
                redoUndoController.switchMainBit(mainBitmap, newBit);
                increaseOpTimes();
            }
            mainBitmap = newBit;
            mainImage.setImageBitmap(mainBitmap);
            mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
        }
    }

    protected void onSaveTaskDone() {
        Intent returnIntent = new Intent();

        if (sourceUri != null) {
            returnIntent.putExtra(ImageEditorIntentBuilder.SOURCE_URI, sourceUri.toString());
        }

        returnIntent.putExtra(ImageEditorIntentBuilder.SOURCE_PATH, sourceFilePath);
        returnIntent.putExtra(ImageEditorIntentBuilder.OUTPUT_PATH, outputFilePath);
        returnIntent.putExtra(IS_IMAGE_EDITED, needSave());

        setResult(RESULT_OK, returnIntent);
        finish();
    }

    protected void doSaveImage() {
        if (!needSave())
            return;

        compositeDisposable.clear();

        Disposable saveImageDisposable = saveImage(mainBitmap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> loadingDialog.show())
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(result -> {
                    if (result) {
                        resetOpTimes();
                        onSaveTaskDone();
                    } else {
                        showToast(R.string.iamutkarshtiwari_github_io_ananas_save_error);
                    }
                }, e -> showToast(R.string.iamutkarshtiwari_github_io_ananas_save_error));

        compositeDisposable.add(saveImageDisposable);
    }

    private Single<Boolean> saveImage(Bitmap finalBitmap) {
        return Single.fromCallable(() -> {
            if (TextUtils.isEmpty(outputFilePath))
                return false;

            return BitmapUtils.saveBitmap(finalBitmap, outputFilePath);
        });
    }

    private void loadImageFromUri(Uri uri) {
        Disposable loadImageDisposable = loadImage(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> loadingDialog.show())
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(processedBitmap -> onFirstLoad(processedBitmap), e -> showToast(R.string.iamutkarshtiwari_github_io_ananas_load_error));

        compositeDisposable.add(loadImageDisposable);
    }

    private void loadImageFromUrl(URL url) {
        Disposable loadImageDisposable = loadImage(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> loadingDialog.show())
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(processedBitmap -> onFirstLoad(processedBitmap), e -> showToast(R.string.iamutkarshtiwari_github_io_ananas_load_error));

        compositeDisposable.add(loadImageDisposable);
    }

    private void loadImageFromFile(String filePath) {
        Disposable loadImageDisposable = loadImage(filePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> {
                    loadingDialog.show();
                    mainMenuFragment.setMenuOptionsClickable(false);
                })
                .doOnSuccess(bitmap -> {
                    mainMenuFragment.setMenuOptionsClickable(true);
                })
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(processedBitmap -> changeMainBitmap(processedBitmap, false), e -> {
                    showToast(R.string.iamutkarshtiwari_github_io_ananas_load_error);
                    Log.wtf("Error", e.getMessage());
                });

        compositeDisposable.add(loadImageDisposable);
    }

    private void onFirstLoad(Bitmap processedBitmap) {
        changeMainBitmap(processedBitmap, false);

        float aspectRatio = (float) mainBitmap.getWidth() / mainBitmap.getHeight();

        if (aspectRatio < aspectRatioMin || (aspectRatioMax > 0 && aspectRatio > aspectRatioMax)) {
            bottomGallery.setCurrentItem(CropFragment.INDEX);
            cropFragment.onShow();
        }
    }

    private Single<Bitmap> loadImage(String filePath) {
        return Single.fromCallable(() -> BitmapUtils.getSampledBitmap(filePath, imageWidth,
                imageHeight));
    }

    private Single<Bitmap> loadImage(Uri uri) {
        return Single.fromCallable(() -> BitmapUtils.decodeSampledBitmap(this, uri,
                imageWidth, imageHeight));
    }

    private Single<Bitmap> loadImage(URL url) {
        return Single.fromCallable(() -> BitmapUtils.getRemoteBitmap(url, imageWidth, imageHeight));
    }

    private void showToast(@StringRes int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();

        if (redoUndoController != null) {
            redoUndoController.onDestroy();
        }

        if (!isPortraitForced) {
            setLockScreenOrientation(false);
        }
    }

    protected void setLockScreenOrientation(boolean lock) {
        if (Build.VERSION.SDK_INT >= 18) {
            setRequestedOrientation(lock ? ActivityInfo.SCREEN_ORIENTATION_LOCKED : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            return;
        }

        if (lock) {
            switch (getWindowManager().getDefaultDisplay().getRotation()) {
                case Surface
                        .ROTATION_0:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                case Surface
                        .ROTATION_90:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Surface
                        .ROTATION_180:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface
                        .ROTATION_270:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
            }
        } else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    public void increaseOpTimes() {
        numberOfOperations++;
        isBeenSaved = false;
    }

    public boolean canAutoExit() {
        return isBeenSaved || numberOfOperations == 0;
    }

    public void resetOpTimes() {
        isBeenSaved = true;
    }

    public Bitmap getMainBit() {
        return mainBitmap;
    }

    private final class BottomGalleryAdapter extends FragmentPagerAdapter {
        BottomGalleryAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int index) {
            switch (index) {
                case MainMenuFragment.INDEX:
                    return mainMenuFragment;
                case FilterListFragment.INDEX:
                    return filterListFragment;
                case CropFragment.INDEX:
                    return cropFragment;
                case RotateFragment.INDEX:
                    return rotateFragment;
                case AddTextFragment.INDEX:
                    return addTextFragment;
                case PaintFragment.INDEX:
                    return paintFragment;
                case BeautyFragment.INDEX:
                    return beautyFragment;
                case BrightnessFragment.INDEX:
                    return brightnessFragment;
                case SaturationFragment.INDEX:
                    return saturationFragment;
            }
            return MainMenuFragment.newInstance();
        }

        @Override
        public int getCount() {
            return 9;
        }
    }

    private final class SaveBtnClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (needSave()) {
                doSaveImage();
            } else {
                onSaveTaskDone();
            }
        }
    }

    private boolean needSave() {
        return numberOfOperations > 0 ||
                (sourceUri != null && sourceUri.getScheme().startsWith("http"));
    }

    private final class ApplyBtnClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            switch (mode) {
                case MODE_FILTER:
                    filterListFragment.applyFilterImage();
                    break;
                case MODE_CROP:
                    cropFragment.applyCropImage();
                    break;
                case MODE_ROTATE:
                    rotateFragment.applyRotateImage();
                    break;
                case MODE_TEXT:
                    addTextFragment.applyTextImage();
                    break;
                case MODE_PAINT:
                    paintFragment.savePaintImage();
                    break;
                case MODE_BEAUTY:
                    beautyFragment.applyBeauty();
                    break;
                case MODE_BRIGHTNESS:
                    brightnessFragment.applyBrightness();
                    break;
                case MODE_SATURATION:
                    saturationFragment.applySaturation();
                    break;
                default:
                    break;
            }
        }
    }
}
