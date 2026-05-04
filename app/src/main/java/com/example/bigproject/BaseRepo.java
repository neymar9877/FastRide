package com.example.bigproject;

import okhttp3.OkHttpClient;
import com.google.gson.Gson;


public class BaseRepo {
    protected static final String SUPABASE_URL =
            "https://tqahzqfcudohlzeqfjqw.supabase.co";

    protected static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRxYWh6cWZjdWRvaGx6ZXFmanF3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIwODAwOTQsImV4cCI6MjA3NzY1NjA5NH0.lZnTkW-EVWdu0hbIjRv4T1-nuc83rqBAY1kjMmqc--Y";

    protected static final OkHttpClient client = new OkHttpClient();
    protected static final Gson gson = new Gson();

    protected BaseRepo() {
        // prevent direct instantiation
    }

    public interface RepoCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}
