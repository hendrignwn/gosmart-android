package com.atc.gosmartlesmagistra.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.atc.gosmartlesmagistra.App;
import com.atc.gosmartlesmagistra.R;
import com.atc.gosmartlesmagistra.adapter.BankListAdapter;
import com.atc.gosmartlesmagistra.adapter.BankSpinnerAdapter;
import com.atc.gosmartlesmagistra.api.OrderApi;
import com.atc.gosmartlesmagistra.api.RequestApi;
import com.atc.gosmartlesmagistra.model.Bank;
import com.atc.gosmartlesmagistra.model.Order;
import com.atc.gosmartlesmagistra.model.Payment;
import com.atc.gosmartlesmagistra.model.TeacherCourse;
import com.atc.gosmartlesmagistra.model.User;
import com.atc.gosmartlesmagistra.model.request.OrderConfirmationRequest;
import com.atc.gosmartlesmagistra.model.response.OrderConfirmationResponse;
import com.atc.gosmartlesmagistra.model.response.OrderResponse;
import com.atc.gosmartlesmagistra.model.response.OrderSuccess;
import com.atc.gosmartlesmagistra.model.response.PaymentBankSuccess;
import com.atc.gosmartlesmagistra.model.response.ResponseSuccess;
import com.atc.gosmartlesmagistra.util.DatabaseHelper;
import com.atc.gosmartlesmagistra.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.sw926.imagefileselector.ErrorResult;
import com.sw926.imagefileselector.ImageFileSelector;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by hendrigunawan on 7/4/17.
 */

public class PrivateOrderActivity extends AppCompatActivity {

    @BindView(R.id.action_left) ImageButton actionLeft;
    @BindView(R.id.teacher) TextView teacherView;
    @BindView(R.id.invoice_number) TextView invoiceNumberView;
    @BindView(R.id.course_name) TextView courseNameView;
    @BindView(R.id.course_section) TextView sectionView;
    @BindView(R.id.final_amount) TextView finalAmountView;
    @BindView(R.id.linear) LinearLayout linearView;
    @BindView(R.id.linear_schedule) LinearLayout linearScheduleView;
    @BindView(R.id.bank_recycler_view) RecyclerView bankRecyclerView;
    @BindView(R.id.swipeContainer) SwipeRefreshLayout swipeContainer;
    @BindView(R.id.bank_account) AutoCompleteTextView mBankAccountView;
    @BindView(R.id.bank_holder_name) AutoCompleteTextView mBankHolderNameView;
    @BindView(R.id.amount) AutoCompleteTextView mAmountView;
    @BindView(R.id.bank_spinner) Spinner bankSpinner;
    @BindView(R.id.image_upload_result) ImageView imageUpload;
    @BindView(R.id.photoText) EditText photoText;
    @BindView(R.id.confirmation_linear) LinearLayout confirmationLinear;
    @BindView(R.id.status) TextView orderStatus;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.checkout_button) AppCompatButton checkOutButton;

    ImageFileSelector mImageFileSelector;
    private int PICK_IMAGE_REQUEST = 1;
    String pathPhoto = "";

    @OnClick(R.id.checkout_button)
    protected void doCheckoutButton(View v) {
        submitConfirmation();
    }

    DatabaseHelper databaseHelper;
    SessionManager sessionManager;
    Order order;
    User user;
    Retrofit retrofit;
    boolean isSubmitSuccess;
    Integer selectedBankSpinner;

    BankListAdapter bankListAdapter;
    List<Bank> bankList;

    @OnClick(R.id.upload_evidence_button)
    protected void doUploadEvidence(View v) {
        capturePhoto();
    }

    @OnClick(R.id.delete_button)
    protected void deleteButton(View v) {
        new AlertDialog.Builder(this)
                .setTitle("Delete This Order?")
                .setMessage("Do you really want to delete this order?")
                .setIcon(android.R.drawable.ic_delete)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        deleting();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    @OnClick(R.id.edit_button)
    protected void editButton(View v) {
        Intent intent = new Intent(this, FillOrderActivity.class);
        intent.putExtra("isEdit", true);
        intent.putExtra("teacherCourse", order.getOrderDetails().get(0).getTeacherCourse());
        intent.putExtra("order", order);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_order);
        ButterKnife.bind(this);
        setActionLeftIcon();

        progressBar.setVisibility(View.INVISIBLE);
        sessionManager = new SessionManager(this);
        databaseHelper = new DatabaseHelper(this);
        user = databaseHelper.getUser(sessionManager.getUserCode());

        linearView.setVisibility(View.INVISIBLE);
        isSubmitSuccess = getIntent().getBooleanExtra("submitSuccess", false);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request newRequest  = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + sessionManager.getUserToken())
                        .addHeader("Content-Type", "application/json")
                        .build();
                return chain.proceed(newRequest);
            }
        }).build();
        retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(App.API)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        if (isSubmitSuccess) {
            loadOrder();
        } else {
            getOrders();
        }

        bankList = new ArrayList<>();
        bankListAdapter = new BankListAdapter(this, bankList);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        bankRecyclerView.setLayoutManager(mLayoutManager);
        bankRecyclerView.setNestedScrollingEnabled(false);
        bankRecyclerView.setHasFixedSize(true);
        bankRecyclerView.setHorizontalScrollBarEnabled(true);
        bankRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(16), true));
        bankRecyclerView.setItemAnimator(new DefaultItemAnimator());
        bankRecyclerView.setAdapter(bankListAdapter);

        swipeContainer.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getOrders();getBanks();
            }
        });
        getBanks();

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1201); // define this constant yourself
        }
        mImageFileSelector = new ImageFileSelector(this);
        mImageFileSelector.setCallback(new ImageFileSelector.Callback() {
            @Override
            public void onError(@NotNull ErrorResult errorResult) {
                switch (errorResult) {
                    case permissionDenied:
                        break;
                    case canceled:
                        break;
                    case error:
                        break;
                }
            }

            @Override
            public void onSuccess(@NotNull String file) {
                Bitmap bm = BitmapFactory.decodeFile(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 70, baos); //bm is the bitmap object
                byte[] b = baos.toByteArray();
                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);

                pathPhoto = encodedImage;
                imageUpload.setImageBitmap(bm);

                String mensagem = "data:image/jpeg;base64," + pathPhoto.replaceAll("\r*\n*", "");
                photoText.setText(mensagem);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mImageFileSelector.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mImageFileSelector.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageFileSelector.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mImageFileSelector.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void loadBankSpinner() {
        BankSpinnerAdapter bankSpinnerAdapter = new BankSpinnerAdapter(this, bankList);
        bankSpinner.setAdapter(bankSpinnerAdapter);
        bankSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBankSpinner = bankList.get(position).getId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getBanks() {
        RequestApi requestApi = retrofit.create(RequestApi.class);
        Call<PaymentBankSuccess> call = requestApi.paymentBanks();
        call.enqueue(new Callback<PaymentBankSuccess>() {
            @Override
            public void onResponse(Call<PaymentBankSuccess> call, Response<PaymentBankSuccess> response) {
                if (response.raw().isSuccessful()) {
                    databaseHelper.createPayment(response.body());
                    bankList.clear();
                    bankList.addAll(response.body().getPayments().get(0).getBanks());
                } else {
                    List<Payment> payments = databaseHelper.getPayments();
                    if (payments.size() > 0) {
                        bankList.clear();
                        bankList.addAll(payments.get(0).getBanks());
                    }
                }
                bankListAdapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
                loadBankSpinner();
            }

            @Override
            public void onFailure(Call<PaymentBankSuccess> call, Throwable t) {
                List<Payment> payments = databaseHelper.getPayments();
                if (payments.size() > 0) {
                    bankList.clear();
                    bankList.addAll(payments.get(0).getBanks());
                }
                bankListAdapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
                loadBankSpinner();
            }
        });
    }

    private void deleting() {
        OrderApi service = retrofit.create(OrderApi.class);
        Call<ResponseSuccess> call = service.orderDelete(sessionManager.getUserCode(), order.getId());
        call.enqueue(new Callback<ResponseSuccess>() {
            @Override
            public void onResponse(Call<ResponseSuccess> call, Response<ResponseSuccess> response) {
                if (response.raw().isSuccessful()) {
                    sessionManager.setKeyHaveAnOrder(false);
                    Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Unable to delete, please try again", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseSuccess> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Unable to delete, please try again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getOrders() {
        OrderApi service = retrofit.create(OrderApi.class);
        Call<OrderSuccess> call = service.orderShow(sessionManager.getUserCode());
        call.enqueue(new Callback<OrderSuccess>() {
            @Override
            public void onResponse(Call<OrderSuccess> call, Response<OrderSuccess> response) {
                if (response.raw().isSuccessful()) {
                    linearView.setVisibility(View.VISIBLE);
                    databaseHelper.createOrder(sessionManager.getUserCode(), response.body());
                    order = response.body().getOrder();
                    sessionManager.setKeyHaveAnOrder(true);
                    Log.i("cranium", order.getStatusIsFormConfirmation() + " " + order.getStatus());
                    if (order.getStatus() == Order.statusConfirmed) {
                        for (int i=0;i<bankList.size();i++) {
                            Integer bankId = bankList.get(i).getId();
                            if (bankId.equals(order.getOrderConfirmation().getBankId())) {
                                selectedBankSpinner = i;
                                bankSpinner.setSelection(selectedBankSpinner);
                                break;
                            }
                        }
                        mBankAccountView.setText(order.getOrderConfirmation().getBankNumber());
                        mBankHolderNameView.setText(order.getOrderConfirmation().getBankBehalfOf());
                        mAmountView.setText(order.getOrderConfirmation().getAmount());
                        Picasso.with(getApplicationContext()).load(App.URL + "files/order-confirmation/" + order.getOrderConfirmation().getUploadBukti()).into(imageUpload);
                    }
                    orderStatus.setText(getResources().getString(R.string.status) + ": " + order.getStatusMessage());
                    teacherView.setText(order.getUser().getFullName());
                    finalAmountView.setText(order.getFormattedFinalAmount());
                    invoiceNumberView.setText(order.getCode());
                    courseNameView.setText(order.getOrderDetails().get(0).getTeacherCourse().getCourse().getName());
                    sectionView.setText(order.getFormattedCreatedAt());
                    mAmountView.setText(order.getFinalAmount());
                    Integer count = 1;
                    linearScheduleView.removeAllViews();
                    for (String onAt : order.getOrderDetails().get(0).getOnDetails()) {
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layoutParams.setMargins(dpToPx(4),dpToPx(4), 0, 0);
                        TextView textView = new TextView(PrivateOrderActivity.this);
                        textView.setLayoutParams(layoutParams);
                        String choose = onAt;
                        Date date = null;
                        try {
                            date = new SimpleDateFormat("yyyy-MM-dd H:m:s", new Locale("id", "ID")).parse(choose);
                            SimpleDateFormat formatted = new SimpleDateFormat("EEEE, dd MMM yyyy H:00", new Locale("id", "ID"));
                            choose = formatted.format(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        textView.setText(getString(R.string.section) + " " + count++ + ": " + choose);
                        linearScheduleView.addView(textView);
                    }
                    return;
                } else if (response.raw().code() == 404) {
                    Toast.makeText(getApplicationContext(), getString(R.string.there_is_no_private_order), Toast.LENGTH_SHORT).show();
                } else if (response.raw().code() == 400) {
                    Gson gson = new GsonBuilder().create();
                    OrderResponse mError =new OrderResponse();
                    try {
                        mError = gson.fromJson(response.errorBody().string(),OrderResponse.class);
                        Toast.makeText(getApplicationContext(), mError.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        // handle failure to read error
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "unable to load, please try again", Toast.LENGTH_LONG).show();
                }
                linearView.setVisibility(View.INVISIBLE);
                sessionManager.setKeyHaveAnOrder(false);
            }

            @Override
            public void onFailure(Call<OrderSuccess> call, Throwable t) {
                loadOrder();
            }
        });
    }

    private void loadOrder() {
        if (!sessionManager.getHaveAnOrder()) {
            linearView.setVisibility(View.INVISIBLE);
            Toast.makeText(getApplicationContext(), getString(R.string.there_is_no_private_order), Toast.LENGTH_SHORT).show();
            return;
        }
        linearView.setVisibility(View.VISIBLE);
        order = databaseHelper.getOrder(sessionManager.getUserCode());
        orderStatus.setText(getResources().getString(R.string.status) + ": " + order.getStatusMessage());
        if (order.getStatus() == Order.statusConfirmed) {
            for (int i=0;i<bankList.size();i++) {
                Integer bankId = bankList.get(i).getId();
                if (bankId.equals(order.getOrderConfirmation().getBankId())) {
                    selectedBankSpinner = i;
                    break;
                }
            }
            bankSpinner.setSelection(selectedBankSpinner);
            mBankAccountView.setText(order.getOrderConfirmation().getBankNumber());
            mBankHolderNameView.setText(order.getOrderConfirmation().getBankBehalfOf());
            mAmountView.setText(order.getOrderConfirmation().getAmount());
            Picasso.with(getApplicationContext()).load(App.URL + "files/order-confirmation/" + order.getOrderConfirmation().getUploadBukti()).into(imageUpload);
        }
        teacherView.setText(order.getUser().getFullName());
        finalAmountView.setText(order.getFormattedFinalAmount());
        invoiceNumberView.setText(order.getCode());
        courseNameView.setText(order.getOrderDetails().get(0).getTeacherCourse().getCourse().getName());
        sectionView.setText(order.getFormattedCreatedAt());
        mAmountView.setText(order.getFinalAmount());
        Integer count = 1;
        for (String onAt : order.getOrderDetails().get(0).getOnDetails()) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(dpToPx(4),dpToPx(4), 0, 0);
            TextView textView = new TextView(this);
            textView.setLayoutParams(layoutParams);
            String choose = onAt;
            Date date = null;
            try {
                date = new SimpleDateFormat("yyyy-MM-dd H:m:s", new Locale("id", "ID")).parse(choose);
                SimpleDateFormat formatted = new SimpleDateFormat("EEEE, dd MMM yyyy H:00", new Locale("id", "ID"));
                choose = formatted.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            textView.setText(getString(R.string.section) + " " + count++ + ": " + choose);
            linearScheduleView.addView(textView);
        }
    }

    private void setActionLeftIcon() {
        final Drawable iconLeft = ContextCompat.getDrawable(this, R.drawable.zzz_arrow_left);
        iconLeft.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        actionLeft.setImageDrawable(iconLeft);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @OnClick(R.id.action_left)
    public void doActionLeft(View view) {
        onBackPressed();
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    private void capturePhoto()
    {
        //mImageFileSelector.setOutPutImageSize(400, 400);
        mImageFileSelector.setQuality(70);
        mImageFileSelector.selectImage(this, PICK_IMAGE_REQUEST);
    }

    private void submitConfirmation() {
        App.hideSoftKeyboard(this);

        // Reset errors.
        bankSpinner.setSelection(0);
        mBankAccountView.setError(null);
        mBankHolderNameView.setError(null);
        mAmountView.setError(null);

        // Store values at the time of the login attempt.
        String bankAccount = mBankAccountView.getText().toString();
        final String bankHolderName = mBankHolderNameView.getText().toString();
        String amount = mAmountView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(amount)) {
            mAmountView.setError(getString(R.string.error_field_required));
            focusView = mAmountView;
            cancel = true;
        }

        if (TextUtils.isEmpty(bankHolderName)) {
            mBankHolderNameView.setError(getString(R.string.error_field_required));
            focusView = mBankHolderNameView;
            cancel = true;
        }

        if (TextUtils.isEmpty(bankAccount)) {
            mBankAccountView.setError(getString(R.string.error_field_required));
            focusView = mBankAccountView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            progressBar.setVisibility(View.VISIBLE);
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            OrderApi service = retrofit.create(OrderApi.class);
            OrderConfirmationRequest request = new OrderConfirmationRequest(selectedBankSpinner, Double.parseDouble(bankAccount), bankHolderName, amount, photoText.getText().toString());
            Call<OrderSuccess> call = service.orderConfirmation(sessionManager.getUserCode(), order.getId(), request);
            call.enqueue(new Callback<OrderSuccess>() {
                @Override
                public void onResponse(Call<OrderSuccess> call, Response<OrderSuccess> response) {

                    if (response.raw().isSuccessful()) {
                        Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();

                        databaseHelper.createOrder(sessionManager.getUserCode(), response.body());
                        order = response.body().getOrder();
                        sessionManager.setKeyHaveAnOrder(true);

                        Intent intent = new Intent(getApplicationContext(), PrivateOrderActivity.class);
                        startActivity(intent);

                    } else if (response.raw().code() == 400) {

                        Gson gson = new GsonBuilder().create();
                        OrderConfirmationResponse mError =new OrderConfirmationResponse();
                        try {
                            mError = gson.fromJson(response.errorBody().string(),OrderConfirmationResponse.class);
                            Toast.makeText(getApplicationContext(), mError.getMessage(), Toast.LENGTH_LONG).show();

                            if (mError.getOrderConfirmationError().getBankNumber() != null) {
                                mBankAccountView.setError(mError.getOrderConfirmationError().getBankNumber());
                                mBankAccountView.requestFocus();
                            }
                            if (mError.getOrderConfirmationError().getBankHolderName() != null) {
                                mBankHolderNameView.setError(mError.getOrderConfirmationError().getBankHolderName());
                                mBankHolderNameView.requestFocus();
                            }
                            if (mError.getOrderConfirmationError().getAmount() != null) {
                                mAmountView.setError(mError.getOrderConfirmationError().getAmount());
                                mAmountView.requestFocus();
                            }
                            if (mError.getOrderConfirmationError().getEvidence() != null) {
                                Toast.makeText(getApplicationContext(), mError.getOrderConfirmationError().getEvidence(), Toast.LENGTH_SHORT).show();
                            }

                        } catch (IOException e) {
                            // handle failure to read error
                        }

                    } else {
                        Toast.makeText(getApplicationContext(), "Confirmation Failed, please try again", Toast.LENGTH_LONG).show();
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onFailure(Call<OrderSuccess> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), "Confirmation failed, please try again", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });

        }
    }
}
