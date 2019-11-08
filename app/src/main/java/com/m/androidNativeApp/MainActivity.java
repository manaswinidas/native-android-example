package com.m.androidNativeApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import com.m.helper.Auth.AuthStateManager;
import com.m.helper.Client;
import com.m.helper.CreateTask;
import com.m.helper.Item;
import com.m.helper.ItemAdapter;
import com.m.helper.MobileService;
import com.m.androidNativeApp.fragment.TaskFields;


import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    public MobileService mobileService;
    public static  ApolloClient client;
    private String taskTitle, taskDescription, taskId;
    private RecyclerView recyclerView;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;
    public Context context;
    public AuthStateManager mAuthStateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuthStateManager = AuthStateManager.getInstance(this);
        context = getApplicationContext();
        mobileService = MobileService.getInstance(context.getApplicationContext());
        String token = "Bearer " + mAuthStateManager.getCurrent().getAccessToken();
        client = Client.setupApollo(mobileService.getGraphqlServer(), token);
        itemList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(this, itemList);

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(itemAdapter);

            }
        });

        subscribeToAddTask();
        subscribeToDeleteTask();
        getTasks();


    }

    public void subscribeToDeleteTask() {
        DeleteTaskSubscription deleteTaskSubscription = DeleteTaskSubscription
                .builder()
                .build();


        client.subscribe(deleteTaskSubscription).execute(new ApolloSubscriptionCall.Callback<DeleteTaskSubscription.Data>() {
            @Override
            public void onResponse(@NotNull Response<DeleteTaskSubscription.Data> response) {
                for (Item item : itemList) {
                    if (item.getId().equals(response.data().taskDeleted.fragments().taskFields.id())) {
                        itemList.remove(item);
                        break;
                    }
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("Failed" + e);
            }

            @Override
            public void onCompleted() {
                System.out.println("Subscribed to DeleteTask");
            }

            @Override
            public void onTerminated() {
                System.out.println("DeleteTask subscription terminated");
            }

            @Override
            public void onConnected() {
                System.out.println("Connected to DeleteTask subscription");
            }
        });
    }

    public void subscribeToAddTask() {
        AddTaskSubscription addTaskSubscription = AddTaskSubscription
                .builder()
                .build();

        client.subscribe(addTaskSubscription).execute(new ApolloSubscriptionCall.Callback<AddTaskSubscription.Data>() {
            @Override
            public void onResponse(@NotNull Response<AddTaskSubscription.Data> response) {
                TaskFields dataReceived = response.data().taskAdded().fragments().taskFields;
                itemList.add(new Item(dataReceived.title(), dataReceived.description(), dataReceived.id()));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("Failed " + e);
            }

            @Override
            public void onCompleted() {
                System.out.println("Subscribed to AddTask");
            }

            @Override
            public void onTerminated() {
                System.out.println("AddTask subscription terminated");
            }

            @Override
            public void onConnected() {
                System.out.println("Connected to AddTask subscription");
            }
        });

    }

    public void deleteTask(View view) {

        final Button button = view.findViewById(R.id.deleteButton);

        final String buttonId = button.getTag().toString();

        DeleteTaskMutation deleteTask = DeleteTaskMutation
                .builder()
                .id(buttonId)
                .build();

        client.mutate(deleteTask).enqueue(new ApolloCall.Callback<DeleteTaskMutation.Data>() {
            @Override
            public void onResponse(@NotNull final Response<DeleteTaskMutation.Data> response) {

                for (Item item : itemList) {
                    if (response.data() != null && item.getId().equals(response.data().deleteTask())) {
                        itemList.remove(item);
                        break;
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println(e);
            }
        });
    }

    public void addTaskActivity(View view) {
        Intent launchActivity1 = new Intent(this, CreateTask.class);
        startActivity(launchActivity1);
    }

    public void getTasks() {

        AllTasksQuery tasksQuery = AllTasksQuery
                .builder()
                .build();

        client.query(tasksQuery).enqueue(new ApolloCall.Callback<AllTasksQuery.Data>() {
            @Override
            public void onResponse(@NotNull Response<AllTasksQuery.Data> response) {
                final int dataLength = response.data().allTasks().size();

                for (int i = 0; i < dataLength; i++) {
                    TaskFields dataReceived = response.data().allTasks().get(i).fragments().taskFields();
                    taskTitle = dataReceived.title();
                    taskDescription = dataReceived.description();
                    taskId = dataReceived.id();
                    itemList.add(new Item(taskTitle, taskDescription, taskId));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println(e);
            }
        });
    }

}
