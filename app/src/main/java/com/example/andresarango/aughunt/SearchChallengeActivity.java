package com.example.andresarango.aughunt;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.andresarango.aughunt.challenge.Challenge;
import com.example.andresarango.aughunt.challenge.ChallengePhoto;
import com.example.andresarango.aughunt.challenge.ChallengePhotoCompleted;
import com.example.andresarango.aughunt.challenge.challenge_dialog_fragment.ChallengeDialogFragment;
import com.example.andresarango.aughunt.challenge.challenge_review_fragments.CompareChallengesFragment;
import com.example.andresarango.aughunt.challenge.challenge_review_fragments.PendingReviewFragment;
import com.example.andresarango.aughunt.challenge.challenge_review_fragments.ReviewChallengesFragment;
import com.example.andresarango.aughunt.challenge.challenges_adapters.created.CreatedChallengeListener;
import com.example.andresarango.aughunt.challenge.challenges_adapters.nearby.ChallengesAdapter;
import com.example.andresarango.aughunt.challenge.challenges_adapters.review.CompletedChallengeListener;
import com.example.andresarango.aughunt.location.DAMLocation;
import com.example.andresarango.aughunt.snapshot_callback.SnapshotHelper;
import com.example.andresarango.aughunt.user.User;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SearchChallengeActivity extends AppCompatActivity implements SearchChallengeHelper,CreatedChallengeListener, SnapshotHelper.SnapshotListener , CompletedChallengeListener {
    private static final int LOCATION_PERMISSION = 1245;

    private ImageView mChallengeImage;
    private TextView mHint;
    private List<Challenge<String>> mChallengeList;
    private TextView mLocation;
    private RecyclerView mRecyclerView;
    private ChallengesAdapter mNearbyChallengesAdapter;
    private Boolean mHasBeenInflated=false;
    private ChallengePhoto mSelectedChallenge;
    private ReviewChallengesFragment mReviewChallengesFragment;
    private CompareChallengesFragment mCompareChallengesFragment;

    private StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    private DAMLocation userLocation;
    private Double radius = 100.0;
    private Map<String, ChallengePhoto> challengeMap = new HashMap<>();
    private List<ChallengePhoto> challengeList = new ArrayList<>();
    private Set<String> submittedChallengeSet = new HashSet<>();

    @BindView(R.id.tv_user_points)
    TextView mUserPointsTv;
    @BindView(R.id.review_number)
    TextView mPendingReview;
    @BindView(R.id.bottom_navigation)
    BottomNavigationView mBottomNav;
    @BindView(R.id.pending_review)
    TextView mPending;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    private int mPendingReviewIndicator = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_challenge);
        ButterKnife.bind(this);
        SnapshotHelper snapshotHelper = new SnapshotHelper(this);
        snapshotHelper.runSnapshot(getApplicationContext());
        initializeViews();
        setUpRecyclerView();

        retrieveUserFromFirebaseAndSetProfile();
        requestPermission();

        mBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {

                    case R.id.create_challenge:

                        Intent createChallenge = new Intent(getApplicationContext(), ChallengeTemplateActivity.class);
                        startActivity(createChallenge);

                        break;
                    case R.id.homepage:
                        Intent homePage = new Intent(getApplicationContext(), SearchChallengeActivity.class);
                        startActivity(homePage);

                        break;
                    case R.id.user_profile:
                        Intent userProfile = new Intent(getApplicationContext(), ProfileActivity.class);
                        startActivity(userProfile);
                        break;
                }
                return true;
            }
        });


    }

    private void retrieveUserFromFirebaseAndSetProfile() {
        rootRef.child("users").child(auth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                System.out.println("USER: " + user.getProfileName());
                mUserPointsTv.setText(user.getUserPoints() + " PTS");
                mUserPointsTv.setTextColor(Color.parseColor("#D81B60"));
                mPendingReview.setTypeface(mPendingReview.getTypeface(), Typeface.BOLD);
                mPendingReview.setText(String.valueOf(mPendingReviewIndicator));
                mPendingReview.setTextColor(Color.parseColor("#D81B60"));
                mPending.setTextColor(Color.parseColor("#D81B60"));
                mPending.setTypeface(mPending.getTypeface(), Typeface.BOLD);
                mPending.setText("Pending review: ");


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void initialize(final Boolean hasBeenInflated) {


        rootRef.child("challenges").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                addChallengeToRecyclerView(dataSnapshot, hasBeenInflated);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (!hasBeenInflated) {
                    updateRecyclerView(dataSnapshot);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //removeChallengeFromRecyclerView(dataSnapshot);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void addChallengeToRecyclerView(DataSnapshot dataSnapshot, final Boolean hasBeenInflated) {
        // Key - value
        String challengeKey = dataSnapshot.getKey();
        ChallengePhoto challenge = dataSnapshot.getValue(ChallengePhoto.class);
        //mPendingReviewIndicator =0;


        if (challenge.getOwnerId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            mPendingReviewIndicator += challenge.getPendingReviews();
            mPendingReview.setText(String.valueOf(mPendingReviewIndicator));
        }
        // Check location
        if (!hasBeenInflated) {
            System.out.println("CALLED GETTING LIST OF SUBMITTED CHALLENGES");
            getListOfSubmittedChallenges(dataSnapshot);

//            if (challenge.getLocation().isWithinRadius(userLocation, radius)) {
//                // Put in challenge map
//                challengeMap.put(challengeKey, challenge);
//                challengeList.add(challengeMap.get(challengeKey));
//
//                mNearbyChallengesAdapter.setChallengeList(challengeList);
//            } else {
//                System.out.println("NOT WITHIN RADIUS");
//            }
        }
    }

    private void getListOfSubmittedChallenges(final DataSnapshot dataSnapshot) {
        final String challengeKey = dataSnapshot.getKey();
        final ChallengePhoto challenge = dataSnapshot.getValue(ChallengePhoto.class);

        rootRef.child("submitted-challenges").child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    submittedChallengeSet.add(snapshot.getKey());
                }

                if (challenge.getLocation().isWithinRadius(userLocation, radius) &&
                        !submittedChallengeSet.contains(challengeKey)) {
                    challengeMap.put(challengeKey, challenge);
                    challengeList.add(challengeMap.get(challengeKey));

                    mNearbyChallengesAdapter.setChallengeList(challengeList);
                } else {
                    System.out.println("NOT WITHIN RADIUS / HAS SUBMITTED");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateRecyclerView(DataSnapshot dataSnapshot) {
        String challengeKey = dataSnapshot.getKey();

        Set<String> challengeKeys = challengeMap.keySet();
        if (challengeKeys.contains(challengeKey)) {
            challengeMap.put(challengeKey, dataSnapshot.getValue(ChallengePhoto.class));


        }

        challengeList.clear();
        for (String key : challengeKeys) {
            challengeList.add(challengeMap.get(key));
        }

        // update recycler view
        mNearbyChallengesAdapter.setChallengeList(challengeList);
    }

    private void setUpRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mNearbyChallengesAdapter = new ChallengesAdapter(this);
        mRecyclerView.setAdapter(mNearbyChallengesAdapter);
    }

    private void initializeViews() {
        mRecyclerView = (RecyclerView) findViewById(R.id.search_challenge_recyclerview);
    }

    @Override
    public void onSearchChallengeClicked(ChallengePhoto challenge) {
        DialogFragment dialogFragment = ChallengeDialogFragment.getInstance(challenge);
        dialogFragment.show(getSupportFragmentManager(), "challenge_fragment");
        mHasBeenInflated = true;
    }

    private void requestPermission() {
        int locationPermission = ContextCompat.checkSelfPermission(SearchChallengeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        boolean locationPermissionIsNotGranted = locationPermission != PackageManager.PERMISSION_GRANTED;
        boolean APILevelIsTwentyThreeOrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        if (locationPermissionIsNotGranted && APILevelIsTwentyThreeOrHigher) {
            marshamallowRequestPermission();
        }
        if (locationPermissionIsNotGranted) {
            ActivityCompat.requestPermissions(SearchChallengeActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION);
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    private void marshamallowRequestPermission() {
        boolean userHasAlreadyRejectedPermission = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
        if (userHasAlreadyRejectedPermission) {
            showMessageOKCancel("We need your location to find nearby challenges, is this too much trouble ?",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(SearchChallengeActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION);
                        }
                    });
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(SearchChallengeActivity.this)
                .setMessage(message)
                .setPositiveButton("NO", onClickListener)
                .setNegativeButton("YES", null)
                .create()
                .show();
    }

    @Override
    public void run(LocationResult locationResult) {
        userLocation = new DAMLocation(locationResult.getLocation().getLatitude(), locationResult.getLocation().getLongitude());
        System.out.println(userLocation.getLat() + " " + userLocation.getLng() + " <--- USER LOCATION");
        initialize(mHasBeenInflated);
    }
public void openPendingReview(View view){

    PendingReviewFragment pendingReviewFragment = new PendingReviewFragment();
    pendingReviewFragment.setmAppCompatActivity(this);
    getSupportFragmentManager().beginTransaction()
            .replace(R.id.search_challenge, pendingReviewFragment)
            .commit();

    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mHasBeenInflated) {
            mPendingReviewIndicator = 0;
            initialize(mHasBeenInflated);
            mHasBeenInflated = false;
        }

        System.out.println("CALLED ON RESUME");



    }

    @Override
    public void onCompletedChallengeClicked(ChallengePhotoCompleted completedChallenge) {
        startCompareChallengeFragment(completedChallenge, mSelectedChallenge);

    }

    @Override
    public void onCreatedChallengeClicked(ChallengePhoto challenge) {
        mSelectedChallenge = challenge;
        startReviewChallengeFragment(challenge);

    }

    private void startReviewChallengeFragment(ChallengePhoto challenge) {
        mReviewChallengesFragment = new ReviewChallengesFragment();
        mReviewChallengesFragment.setChallengeToReview(challenge);
        mReviewChallengesFragment.setmListener(this);

       getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.search_challenge, mReviewChallengesFragment)
                .addToBackStack(null)
                .commit();
    }

    private void startCompareChallengeFragment(ChallengePhotoCompleted completedChallenge, ChallengePhoto challenge) {
        mCompareChallengesFragment = new CompareChallengesFragment();
        mCompareChallengesFragment.setCompletedChallenge(completedChallenge);
        mCompareChallengesFragment.setCurrentChallenge(challenge);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.search_challenge, mCompareChallengesFragment)
                .addToBackStack(null)
                .commit();
    }
}













