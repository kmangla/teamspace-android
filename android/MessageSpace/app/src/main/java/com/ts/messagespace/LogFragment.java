package com.ts.messagespace;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by pratyus on 9/20/15.
 */
public class LogFragment extends Fragment {
    private static View mRootView;
    private TextView logs;
    private Button deleteLogs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.log_fragment, container, false);
        }
        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null) {
            parent.removeView(mRootView);
        }

        logs = (TextView) mRootView.findViewById(R.id.logs);
        logs.setText(Utils.retrieveDevLogs(getActivity()));

        Button deleteLogs = (Button) mRootView.findViewById(R.id.delete_logs);
        deleteLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.clearDevLogs(getActivity());
                logs.setText("");
            }
        });

        getActivity().setTitle(R.string.action_settings);

        return mRootView;
    }
}
