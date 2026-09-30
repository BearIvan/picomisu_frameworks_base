// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.util;

import static android.net.DnsResolver.FLAG_NO_CACHE_LOOKUP;
import static android.net.DnsResolver.TYPE_A;
import static android.net.DnsResolver.TYPE_AAAA;

import android.annotation.NonNull;
import android.net.DnsResolver;
import android.net.Network;
import android.net.TrafficStats;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Collection of utilities for DNS queries (copy of the NetworkStack DnsUtils used by the PICO
 * network probes).
 *
 * @hide
 */
public class DnsUtils {
    private static final String TAG = DnsUtils.class.getSimpleName();

    /** Query type used to resolve both A and AAAA records in a single query. */
    public static final int TYPE_ADDRCONFIG = -1;

    /**
     * Return both A and AAAA query results regardless the ip address type of the giving network.
     * Used for probing in NetworkMonitor.
     */
    @NonNull
    public static InetAddress[] getAllByName(@NonNull final DnsResolver dnsResolver,
            @NonNull final Network network, @NonNull String host, int timeout)
            throws UnknownHostException {
        final List<InetAddress> result = new ArrayList<InetAddress>();

        try {
            result.addAll(Arrays.asList(
                    getAllByName(dnsResolver, network, host, TYPE_AAAA, FLAG_NO_CACHE_LOOKUP,
                    timeout)));
        } catch (UnknownHostException e) {
            // Might happen if the host is v4-only, still need to query TYPE_A
        }
        try {
            result.addAll(Arrays.asList(
                    getAllByName(dnsResolver, network, host, TYPE_A, FLAG_NO_CACHE_LOOKUP,
                    timeout)));
        } catch (UnknownHostException e) {
            // Might happen if the host is v6-only, still need to return AAAA answers
        }
        if (result.size() == 0) {
            throw new UnknownHostException(host);
        }
        return result.toArray(new InetAddress[0]);
    }

    /**
     * Return dns query result based on the given QueryType(TYPE_A, TYPE_AAAA) or TYPE_ADDRCONFIG.
     */
    @NonNull
    public static InetAddress[] getAllByName(@NonNull final DnsResolver dnsResolver,
            @NonNull final Network network, @NonNull final String host, int type, int flag,
            int timeoutMs) throws UnknownHostException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<List<InetAddress>> resultRef = new AtomicReference<>();

        final DnsResolver.Callback<List<InetAddress>> callback =
                new DnsResolver.Callback<List<InetAddress>>() {
            @Override
            public void onAnswer(List<InetAddress> answer, int rcode) {
                if (rcode == 0) {
                    resultRef.set(answer);
                }
                latch.countDown();
            }

            @Override
            public void onError(@NonNull DnsResolver.DnsException e) {
                Log.d(TAG, "DNS error resolving " + host + ": " + e.getMessage());
                latch.countDown();
            }
        };
        final int oldTag = TrafficStats.getAndSetThreadStatsTag(
                0xFFFFFF81 /* TrafficStatsConstants.TAG_SYSTEM_PROBE */);

        if (type == TYPE_ADDRCONFIG) {
            dnsResolver.query(network, host, flag, r -> r.run(), null /* cancellationSignal */,
                    callback);
        } else {
            dnsResolver.query(network, host, type, flag, r -> r.run(),
                    null /* cancellationSignal */, callback);
        }

        TrafficStats.setThreadStatsTag(oldTag);

        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        final List<InetAddress> result = resultRef.get();
        if (result == null || result.size() == 0) {
            throw new UnknownHostException(host);
        }

        return result.toArray(new InetAddress[0]);
    }
}
