package com.example.andresarango.aughunt.review;


import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.daprlabs.aaron.swipedeck.SwipeDeck;
import com.example.andresarango.aughunt.R;
import com.example.andresarango.aughunt._models.ChallengePhoto;
import com.example.andresarango.aughunt._models.ChallengePhotoCompleted;
import com.example.andresarango.aughunt._models.ChallengePhotoSubmitted;
import com.example.andresarango.aughunt._models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Millochka on 3/6/17.
 */

public class ReviewChallengesFragment extends Fragment implements SwipeDeck.SwipeDeckCallback {
    private ChallengePhoto mChallengeToReview;

    private ReviewSwipeAdapter mSwipeAdapter = new ReviewSwipeAdapter();

    Deque<ChallengePhotoCompleted> mCompletedChallengeDeck = new LinkedList<>();

    @BindView(R.id.swipe_deck) SwipeDeck mSwipeDeck;
    @BindView(R.id.tv_user_points) TextView mUserPointsTv;
    @BindView(R.id.review_number) TextView mPendingReview;
    @BindView(R.id.pending_review) TextView mPending;
    @BindView(R.id.btn_review_decline) Button mDeclineBtn;
    @BindView(R.id.btn_review_accept) Button mAcceptBtn;

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    private StorageReference storageRef = FirebaseStorage.getInstance().getReference();

    private Map<String, ChallengePhotoCompleted> challengeMap = new HashMap<>();
    private PopFragmentListener mListener;
    private int mPendingReviewIndicator = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_challenges_review, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        initializeSwiperView();
        retrieveUserFromFirebaseAndSetProfile();

        mDeclineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSwipeDeck.swipeTopCardLeft(180);
            }
        });

        mAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSwipeDeck.swipeTopCardRight(180);
            }
        });

    }

    public void setChallengeToReview(ChallengePhoto challengeToReview) {
        mChallengeToReview = challengeToReview;
    }


    public void initializeSwiperView() {

        mSwipeDeck.setCallback(this);
        mSwipeDeck.setLeftImage(R.id.left_image);
        mSwipeDeck.setRightImage(R.id.right_image);

        rootRef.child("completed-challenges").child(mChallengeToReview.getChallengeId()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                addChallengeToSwiperView(dataSnapshot);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                updateSwiperView(dataSnapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mSwipeDeck.setAdapter(mSwipeAdapter);
    }

    private void retrieveUserFromFirebaseAndSetProfile() {
        rootRef.child("users").child(auth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                System.out.println("USER: " + user.getProfileName());
                mUserPointsTv.setText(user.getUserPoints() + " PTS");
                mPendingReview.setTypeface(mPendingReview.getTypeface(), Typeface.BOLD);
                mPendingReview.setText("2");
                mPending.setTypeface(mPending.getTypeface(), Typeface.BOLD);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        rootRef.child("challenges").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ChallengePhoto challenge = dataSnapshot.getValue(ChallengePhoto.class);
                if (challenge.getOwnerId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    mPendingReviewIndicator += challenge.getPendingReviews();
                    mPendingReview.setText(String.valueOf(mPendingReviewIndicator));
                    if (mPendingReviewIndicator > 0) {
                        mPendingReview.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                        mPending.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                    } else {
                        mPendingReview.setTextColor(ContextCompat.getColor(getContext(), R.color.lightGrey));
                        mPending.setTextColor(ContextCompat.getColor(getContext(), R.color.lightGrey));
                    }
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateSwiperView(DataSnapshot dataSnapshot) {
        String challengeKey = dataSnapshot.getKey();
        List<ChallengePhotoCompleted> challengeList = new ArrayList<>();

        Set<String> challengeKeys = challengeMap.keySet();
        if (challengeKeys.contains(challengeKey)) {
            challengeMap.put(challengeKey, dataSnapshot.getValue(ChallengePhotoCompleted.class));
        }

        challengeList.clear();
        for (String key : challengeKeys) {
            challengeList.add(challengeMap.get(key));
        }

        mCompletedChallengeDeck.clear();
        mCompletedChallengeDeck.addAll(challengeList);

        mSwipeAdapter.setCompletedChallengeList(challengeList);
    }

    private void addChallengeToSwiperView(DataSnapshot dataSnapshot) {
        // Key - value
        String challengeKey = dataSnapshot.getKey();
        ChallengePhotoCompleted challenge = dataSnapshot.getValue(ChallengePhotoCompleted.class);


        System.out.println("BOOM BOOM");
        // Put in challenge map
        challengeMap.put(challengeKey, challenge);
        mCompletedChallengeDeck.addFirst(challenge);
        mSwipeAdapter.addChallenge(challenge);


    }

    @Override
    public void cardSwipedLeft(long stableId) {
        ChallengePhotoCompleted completed = mCompletedChallengeDeck.removeLast();
//        removeCompletedChallengeFromFirebase(completed);
        decrementPendingReviewCounter();
        updateUsersSubmittedChallenge(completed, false);
        updateUsersReviewedChallenge();

        if (mCompletedChallengeDeck.isEmpty()) {
            mListener.popFragment(this);
        }
    }

    @Override
    public void cardSwipedRight(long stableId) {
        ChallengePhotoCompleted completed = mCompletedChallengeDeck.removeLast();
//        removeCompletedChallengeFromFirebase(completed);
        decrementPendingReviewCounter();
        updateUserPoints(completed);
        updateUsersSubmittedChallenge(completed, true);
        updateUsersReviewedChallenge();

        if (mCompletedChallengeDeck.isEmpty()) {
            mListener.popFragment(this);
        }
    }

    private void updateUsersReviewedChallenge() {
        rootRef.child("users").child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User currentUser = dataSnapshot.getValue(User.class);
                currentUser.setNumberOfReviewedChallenges(currentUser.getNumberOfReviewedChallenges() + 1);
                rootRef.child("users").child(auth.getCurrentUser().getUid()).setValue(currentUser);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateUsersSubmittedChallenge(final ChallengePhotoCompleted completed, final boolean isAccepted) {
        rootRef.child("submitted-challenges").child(completed.getPlayerId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.getKey().equals(completed.getOriginalChallengeId())) {
                        ChallengePhotoSubmitted submittedChallenge = snapshot.getValue(ChallengePhotoSubmitted.class);
                        submittedChallenge.setAccepted(isAccepted);
                        submittedChallenge.setReviewed(true);
                        rootRef.child("submitted-challenges").child(completed.getPlayerId()).child(snapshot.getKey()).setValue(submittedChallenge);
                        
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void removeCompletedChallengeFromFirebase(ChallengePhotoCompleted completedChallenge) {
        // Delete database value
        rootRef.child("completed-challenges")
                .child(mChallengeToReview.getChallengeId())
                .child(completedChallenge.getCompletedChallengeId())
                .removeValue();

        // Delete storage image
        storageRef.child("challenges")
                .child(mChallengeToReview.getChallengeId())
                .child(completedChallenge.getCompletedChallengeId())
                .delete();
    }

    private void decrementPendingReviewCounter() {
        rootRef.child("challenges").child(mChallengeToReview.getChallengeId()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                ChallengePhoto challenge = mutableData.getValue(ChallengePhoto.class);
                challenge.setPendingReviews(challenge.getPendingReviews() - 1);
                mutableData.setValue(challenge);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }

    public void setPopFragmentListener(PopFragmentListener listener) {
        mListener = listener;
    }

    @Override
    public void onStop() {
        super.onStop();
        System.out.println("CALLED ON STOP");
        mListener.setTabLayoutVisibile();
    }

    private void updateUserPoints(final ChallengePhotoCompleted completed) {
        rootRef.child("users").child(completed.getPlayerId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                user.setUserPoints(user.getUserPoints()+1);
                rootRef.child("users").child(completed.getPlayerId()).setValue(user);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
