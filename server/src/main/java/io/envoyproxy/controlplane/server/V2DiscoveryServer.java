package io.envoyproxy.controlplane.server;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import io.envoyproxy.controlplane.cache.ConfigWatcher;
import io.envoyproxy.controlplane.cache.Resources;
import io.envoyproxy.controlplane.cache.XdsRequest;
import io.envoyproxy.controlplane.server.serializer.DefaultProtoResourcesSerializer;
import io.envoyproxy.controlplane.server.serializer.ProtoResourcesSerializer;
import io.envoyproxy.envoy.api.v2.ClusterDiscoveryServiceGrpc;
import io.envoyproxy.envoy.api.v2.DiscoveryRequest;
import io.envoyproxy.envoy.api.v2.DiscoveryResponse;
import io.envoyproxy.envoy.api.v2.EndpointDiscoveryServiceGrpc;
import io.envoyproxy.envoy.api.v2.ListenerDiscoveryServiceGrpc;
import io.envoyproxy.envoy.api.v2.RouteDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v2.SecretDiscoveryServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Deprecated
public class V2DiscoveryServer extends DiscoveryServer<DiscoveryRequest, DiscoveryResponse> {

  public V2DiscoveryServer(ConfigWatcher configWatcher) {
    this(Collections.emptyList(), configWatcher);
  }

  public V2DiscoveryServer(DiscoveryServerCallbacks callbacks,
      ConfigWatcher configWatcher) {
    this(Collections.singletonList(callbacks), configWatcher);
  }

  public V2DiscoveryServer(
      List<DiscoveryServerCallbacks> callbacks,
      ConfigWatcher configWatcher) {
    this(callbacks, configWatcher, new DefaultExecutorGroup(),
        new DefaultProtoResourcesSerializer());
  }

  public V2DiscoveryServer(List<DiscoveryServerCallbacks> callbacks,
      ConfigWatcher configWatcher, ExecutorGroup executorGroup, ProtoResourcesSerializer protoResourcesSerializer) {
    super(callbacks, configWatcher, executorGroup, protoResourcesSerializer);
  }

  /**
   * Returns an ADS implementation that uses this server's {@link ConfigWatcher}.
   */
  public AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceImplBase getAggregatedDiscoveryServiceImpl() {
    return new AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceImplBase() {
      @Override
      public StreamObserver<DiscoveryRequest> streamAggregatedResources(
          StreamObserver<DiscoveryResponse> responseObserver) {

        return createRequestHandler(responseObserver, true, ANY_TYPE_URL);
      }
    };
  }

  /**
   * Returns a CDS implementation that uses this server's {@link ConfigWatcher}.
   */
  public ClusterDiscoveryServiceGrpc.ClusterDiscoveryServiceImplBase getClusterDiscoveryServiceImpl() {
    return new ClusterDiscoveryServiceGrpc.ClusterDiscoveryServiceImplBase() {
      @Override
      public StreamObserver<DiscoveryRequest> streamClusters(
          StreamObserver<DiscoveryResponse> responseObserver) {

        return createRequestHandler(responseObserver, false, Resources.V2.CLUSTER_TYPE_URL);
      }
    };
  }

  /**
   * Returns an EDS implementation that uses this server's {@link ConfigWatcher}.
   */
  public EndpointDiscoveryServiceGrpc.EndpointDiscoveryServiceImplBase getEndpointDiscoveryServiceImpl() {
    return new EndpointDiscoveryServiceGrpc.EndpointDiscoveryServiceImplBase() {
      @Override
      public StreamObserver<DiscoveryRequest> streamEndpoints(
          StreamObserver<DiscoveryResponse> responseObserver) {

        return createRequestHandler(responseObserver, false, Resources.V2.ENDPOINT_TYPE_URL);
      }
    };
  }

  /**
   * Returns a LDS implementation that uses this server's {@link ConfigWatcher}.
   */
  public ListenerDiscoveryServiceGrpc.ListenerDiscoveryServiceImplBase getListenerDiscoveryServiceImpl() {
    return new ListenerDiscoveryServiceGrpc.ListenerDiscoveryServiceImplBase() {
      @Override
      public StreamObserver<DiscoveryRequest> streamListeners(
          StreamObserver<DiscoveryResponse> responseObserver) {

        return createRequestHandler(responseObserver, false, Resources.V2.LISTENER_TYPE_URL);
      }
    };
  }

  /**
   * Returns a RDS implementation that uses this server's {@link ConfigWatcher}.
   */
  public RouteDiscoveryServiceGrpc.RouteDiscoveryServiceImplBase getRouteDiscoveryServiceImpl() {
    return new RouteDiscoveryServiceGrpc.RouteDiscoveryServiceImplBase() {
      @Override
      public StreamObserver<DiscoveryRequest> streamRoutes(
          StreamObserver<DiscoveryResponse> responseObserver) {

        return createRequestHandler(responseObserver, false, Resources.V2.ROUTE_TYPE_URL);
      }
    };
  }

  /**
   * Returns a SDS implementation that uses this server's {@link ConfigWatcher}.
   */
  public SecretDiscoveryServiceGrpc.SecretDiscoveryServiceImplBase getSecretDiscoveryServiceImpl() {
    return new SecretDiscoveryServiceGrpc.SecretDiscoveryServiceImplBase() {
      @Override
      public StreamObserver<DiscoveryRequest> streamSecrets(
          StreamObserver<DiscoveryResponse> responseObserver) {
        return createRequestHandler(responseObserver, false, Resources.V2.SECRET_TYPE_URL);
      }
    };
  }

  @Override
  protected XdsRequest wrapXdsRequest(DiscoveryRequest request) {
    return XdsRequest.create(request);
  }

  @Override
  protected void runStreamRequestCallbacks(long streamId, DiscoveryRequest discoveryRequest) {
    callbacks.forEach(
        cb -> cb.onV2StreamRequest(streamId, discoveryRequest));
  }

  @Override
  protected void runStreamResponseCallbacks(long streamId, XdsRequest request,
      DiscoveryResponse discoveryResponse) {
    Preconditions.checkArgument(request.v2Request() != null);
    callbacks.forEach(
        cb -> cb.onStreamResponse(streamId,
            request.v2Request(),
            discoveryResponse));
  }

  @Override
  protected DiscoveryResponse makeResponse(String version, Collection<Any> resources,
      String typeUrl,
      String nonce) {
    return DiscoveryResponse.newBuilder()
        .setNonce(nonce)
        .setVersionInfo(version)
        .addAllResources(resources)
        .setTypeUrl(typeUrl)
        .build();
  }
}
