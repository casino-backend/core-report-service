package com.core.report.proto;



public interface UserServiceGrpc {/*

  UserServiceBlockingStub getUserServiceBlockingStub();

  UserServiceFutureStub getUserServiceFutureStub();

  UserServiceStub getUserServiceStub();

  public interface UserServiceBlockingStub {
    public io.grpc.examples.game.GetUserResponse getUser(io.grpc.examples.game.GetUserRequest request);

    public io.grpc.examples.game.GetParentsResponse getUserParents(io.grpc.examples.game.GetParentsRequest request);
  }

  public interface UserServiceFutureStub {
    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.game.GetUserResponse> getUser(
        io.grpc.examples.game.GetUserRequest request);

    public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.game.GetParentsResponse> getUserParents(
        io.grpc.examples.game.GetParentsRequest request);
  }

  public interface UserServiceStub {
    public void getUser(io.grpc.examples.game.GetUserRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.game.GetUserResponse> responseObserver);

    public void getUserParents(io.grpc.examples.game.GetParentsRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.game.GetParentsResponse> responseObserver);
  }

  public static abstract class UserServiceImplBase implements BindableService {
    public void getUser(io.grpc.examples.game.GetUserRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.game.GetUserResponse> responseObserver) {
      io.grpc.Status status = io.grpc.Status.UNIMPLEMENTED.withDescription("Method getUser not implemented");
      responseObserver.onError(status.asRuntimeException());
    }

    public void getUserParents(io.grpc.examples.game.GetParentsRequest request,
        io.grpc.stub.StreamObserver<io.grpc.examples.game.GetParentsResponse> responseObserver) {
      io.grpc.Status status = io.grpc.Status.UNIMPLEMENTED.withDescription("Method getUserParents not implemented");
      responseObserver.onError(status.asRuntimeException());
    }

    public final ServerServiceDefinition bindService() {
      return ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getGetUserMethod(), ServerCalls.asyncUnaryCall(new MethodHandlers<io.grpc.examples.game.GetUserRequest, io.grpc.examples.game.GetUserResponse>(
              this, METHODID_GET_USER)))
          .addMethod(getGetUserParentsMethod(), ServerCalls.asyncUnaryCall(new MethodHandlers<io.grpc.examples.game.GetParentsRequest, io.grpc.examples.game.GetParentsResponse>(
              this, METHODID_GET_USER_PARENTS)))
          .build();
    }

    public final ServiceDescriptor getServiceDescriptor() {
      return UserServiceGrpc.getServiceDescriptor();
    }

    public final MethodDescriptor<io.grpc.examples.game.GetUserRequest, io.grpc.examples.game.GetUserResponse> getGetUserMethod() {
      return UserServiceGrpc.getGetUserMethod();
    }

    public final MethodDescriptor<io.grpc.examples.game.GetParentsRequest, io.grpc.examples.game.GetParentsResponse> getGetUserParentsMethod() {
      return UserServiceGrpc.getGetUserParentsMethod();
    }

    public static final class MethodHandlers<Req, Resp> implements ServerCalls.UnaryMethod<Req, Resp>,
        ServerCalls.ServerStreamingMethod<Req, Resp>, ServerCalls.ClientStreamingMethod<Req, Resp>,
        ServerCalls.BidiStreamingMethod<Req, Resp> {
      private final UserServiceImplBase serviceImpl;
      private final int methodId;

      public MethodHandlers(UserServiceImplBase serviceImpl, int methodId) {
        this.serviceImpl = serviceImpl;
        this.methodId = methodId;
      }

      @Override
      public void invoke(Req request, StreamObserver<Resp> responseObserver) {
        switch (methodId) {
          case METHODID_GET_USER:
            serviceImpl.getUser((io.grpc.examples.game.GetUserRequest) request,
                (io.grpc.stub.StreamObserver<io.grpc.examples.game.GetUserResponse>) responseObserver);
            break;
          case METHODID_GET_USER_PARENTS:
            serviceImpl.getUserParents((io.grpc.examples.game.GetParentsRequest) request,
                (io.grpc.stub.StreamObserver<io.grpc.examples.game.GetParentsResponse>) responseObserver);
            break;
          default:
            throw new AssertionError();
        }
      }

      @Override
      public StreamObserver<Req> invoke(StreamObserver<Resp> responseObserver) {
        switch (methodId) {
          default:
            throw new AssertionError();
        }
      }
    }

    private static final int METHODID_GET_USER = 0;
    private static final int METHODID_GET_USER_PARENTS = 1;

    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_GET_USER,
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new io.grpc.stub.ServerCalls.UnaryMethod<
                io.grpc.examples.game.GetUserRequest,
                io.grpc.examples.game.GetUserResponse>() {
                @java.lang.Override
                public void invoke(
                    io.grpc.examples.game.GetUserRequest request,
                    io.grpc.stub.StreamObserver<io.grpc.examples.game.GetUserResponse> responseObserver) {
                  serviceImpl.getUser(request, responseObserver);
                }
              }))
          .addMethod(
            METHOD_GET_USER_PARENTS,
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new io.grpc.stub.ServerCalls.UnaryMethod<
                io.grpc.examples.game.GetParentsRequest,
                io.grpc.examples.game.GetParentsResponse>() {
                @java.lang.Override
                public void invoke(
                    io.grpc.examples.game.GetParentsRequest request,
                    io.grpc.stub.StreamObserver<io.grpc.examples.game.GetParentsResponse> responseObserver) {
                  serviceImpl.getUserParents(request, responseObserver);
                }
              }))
          .build();
    }
  }

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    return new io.grpc.ServiceDescriptor(SERVICE_NAME,
        METHOD_GET_USER,
        METHOD_GET_USER_PARENTS);
  }

  public static final String SERVICE_NAME = "game.UserService";
  private static final int METHODID_GET_USER = 0;
  private static final int METHODID_GET_USER_PARENTS = 1;

  public static MethodDescriptor<io.grpc.examples.game.GetUserRequest, io.grpc.examples.game.GetUserResponse> getGetUserMethod() {
    MethodDescriptor<io.grpc.examples.game.GetUserRequest, io.grpc.examples.game.GetUserResponse> getGetUserMethod;
    if ((getGetUserMethod = UserServiceGrpc.getGetUserMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetUserMethod = UserServiceGrpc.getGetUserMethod) == null) {
          UserServiceGrpc.getGetUserMethod = getGetUserMethod =
              MethodDescriptor.<io.grpc.examples.game.GetUserRequest, io.grpc.examples.game.GetUserResponse>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.game.GetUserRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.game.GetUserResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserServiceMethodDescriptorSupplier("GetUser"))
              .build();
        }
      }
    }
    return getGetUserMethod;
  }

  private static volatile MethodDescriptor<io.grpc.examples.game.GetUserRequest, io.grpc.examples.game.GetUserResponse> getGetUserMethod;

  public static MethodDescriptor<io.grpc.examples.game.GetParentsRequest, io.grpc.examples.game.GetParentsResponse> getGetUserParentsMethod() {
    MethodDescriptor<io.grpc.examples.game.GetParentsRequest, io.grpc.examples.game.GetParentsResponse> getGetUserParentsMethod;
    if ((getGetUserParentsMethod = UserServiceGrpc.getGetUserParentsMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetUserParentsMethod = UserServiceGrpc.getGetUserParentsMethod) == null) {
          UserServiceGrpc.getGetUserParentsMethod = getGetUserParentsMethod =
              MethodDescriptor.<io.grpc.examples.game.GetParentsRequest, io.grpc.examples.game.GetParentsResponse>newBuilder()
              .setType(MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetUserParents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.game.GetParentsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.grpc.examples.game.GetParentsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new UserServiceMethodDescriptorSupplier("GetUserParents"))
              .build();
        }
      }
    }
    return getGetUserParentsMethod;
  }

  private static volatile MethodDescriptor<io.grpc.examples.game.GetParentsRequest, io.grpc.examples.game.GetParentsResponse> getGetUserParentsMethod;

  private static final class UserServiceMethodDescriptorSupplier
      extends UserServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    UserServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public MethodDescriptor<io.grpc.examples.game.GetUserRequest, io.grpc.examples.game.GetUserResponse> getMethodDescriptor() {
      if (methodName.equals("GetUser")) {
        return UserServiceGrpc.getGetUserMethod();
      }
      if (methodName.equals("GetUserParents")) {
        return UserServiceGrpc.getGetUserParentsMethod();
      }
      return null;
    }
  }

  private static abstract class UserServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    UserServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.grpc.examples.game.Game.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("UserService");
    }
  }

  private static final class UserServiceFileDescriptorSupplier
      extends UserServiceBaseDescriptorSupplier {
    UserServiceFileDescriptorSupplier() {}
  }

  private static final class UserServiceMethodDescriptorSupplier
      extends UserServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    UserServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      if (methodName.equals("GetUser")) {
        return io.grpc.examples.game.UserServiceGrpc.getGetUserMethod();
      }
      if (methodName.equals("GetUserParents")) {
        return io.grpc.examples.game.UserServiceGrpc.getGetUserParentsMethod();
      }
      return null;
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (UserServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = new io.grpc.ServiceDescriptor(SERVICE_NAME,
              getGetUserMethod(),
              getGetUserParentsMethod());
        }
      }
    }
    return result;
  }*/
}


