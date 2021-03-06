package com.example.andresarango.aughunt.leaderboard;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.andresarango.aughunt.R;
import com.example.andresarango.aughunt._models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Millochka on 3/14/17.
 */

class LeaderBoardAdapter extends RecyclerView.Adapter {

    private List<User> mUserList= new ArrayList<>();

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.vh_leaderboard, parent, false);
        return new LeaderBoardViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LeaderBoardViewHolder leaderBoardViewHolder=(LeaderBoardViewHolder) holder;
        leaderBoardViewHolder.bind(mUserList.get(position));

    }

    @Override
    public int getItemCount() {
        return mUserList.size();
    }

    public void setUserList(List<User> userList) {

        Comparator<User> pointsComparator = new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return Integer.valueOf(o2.getUserPoints()).compareTo(o1.getUserPoints());
            }
        };

        Collections.sort(userList, pointsComparator);
        this.mUserList = userList;

        notifyDataSetChanged();
    }

    public void addUserToList(User user) {
        mUserList.add(user);
        Comparator<User> pointsComparator = new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return Integer.valueOf(o2.getUserPoints()).compareTo(o1.getUserPoints());
            }
        };

        Collections.sort(mUserList, pointsComparator);

        notifyDataSetChanged();
    }
}
