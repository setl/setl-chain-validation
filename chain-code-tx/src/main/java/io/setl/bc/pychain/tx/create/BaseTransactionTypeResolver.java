/* <notice>
 
    SETL Blockchain
    Copyright (C) 2021 SETL Ltd
 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License, version 3, as
    published by the Free Software Foundation.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
 
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 
</notice> */
package io.setl.bc.pychain.tx.create;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

import io.setl.common.CommonPy.TxType;
import io.setl.util.CopyOnWriteSet;

/**
 * @author Simon Greatrix on 13/12/2019.
 */
public class BaseTransactionTypeResolver implements TypeIdResolver {

  static final CopyOnWriteSet<Class<? extends BaseTransaction>> ALL_SUB_TYPES = new CopyOnWriteSet<>();

  private static final List<Class<? extends BaseTransaction>> CORE_SUB_TYPES = Arrays.asList(
      AddressDelete.class,
      AddressPermissions.class,
      AddXChain.class,
      AssetClassDelete.class,
      AssetClassRegister.class,
      AssetClassUpdate.class,
      AssetTransfer.class,
      AssetIssue.class,
      AssetIssueAndEncumber.class,
      AssetTransfer.class,
      AssetTransferXChain.class,
      Bond.class,
      CommitToContract.class,
      DeleteXChain.class,
      Encumber.class,
      ExerciseEncumbrance.class,
      IssuerTransfer.class,
      LockAsset.class,
      LockHolding.class,
      Memo.class,
      NamespaceDelete.class,
      NamespaceRegister.class,
      NamespaceTransfer.class,
      NewContract.class,
      NullTX.class,
      PoaAdd.class,
      PoaDelete.class,
      PrivilegedOperation.class,
      RegisterAddress.class,
      TransferFromMany.class,
      TransferToMany.class,
      UnBond.class,
      UnEncumber.class,
      UnLockAsset.class,
      UnlockHolding.class,
      PoaAssetClassDelete.class,
      PoaAssetClassRegister.class,
      PoaAssetIssue.class,
      PoaAssetIssueAndEncumber.class,
      PoaAssetTransferXChain.class,
      PoaCommitToContract.class,
      PoaEncumber.class,
      PoaExerciseEncumbrance.class,
      PoaIssuerTransfer.class,
      PoaLockAsset.class,
      PoaLockHolding.class,
      PoaNamespaceDelete.class,
      PoaNamespaceRegister.class,
      PoaNamespaceTransfer.class,
      PoaNewContract.class,
      PoaTransferFromMany.class,
      PoaTransferToMany.class,
      PoaUnEncumber.class,
      PoaUnLockAsset.class,
      PoaUnlockHolding.class
  );


  public static void addTypes(List<Class<? extends BaseTransaction>> extraTypes) {
    ALL_SUB_TYPES.addAll(extraTypes);
  }


  static {
    ALL_SUB_TYPES.addAll(CORE_SUB_TYPES);
  }

  private final Map<TxType, JavaType> decode = new EnumMap<>(TxType.class);

  private boolean isInitialised = false;


  @Override
  public String getDescForKnownTypeIds() {
    return "Transaction Types";
  }


  @Override
  public Id getMechanism() {
    return JsonTypeInfo.Id.NAME;
  }


  @Override
  public String idFromBaseType() {
    throw new UnsupportedOperationException();
  }


  @Override
  public String idFromValue(Object value) {
    return ((BaseTransaction) value).getTxType().getExternalName();
  }


  @Override
  public String idFromValueAndType(Object value, Class<?> suggestedType) {
    return idFromValue(value);
  }


  @Override
  public void init(JavaType baseType) {
    // do nothing - the only type this applies to is BaseTransaction, and we need a DatabindContext to create JavaTypes.
  }


  /**
   * Initialise once, as soon as we have a DatabindContext.
   *
   * @param context the context
   */
  private synchronized void initialise(DatabindContext context) {
    if (isInitialised) {
      return;
    }

    ALL_SUB_TYPES.addAll(CORE_SUB_TYPES);

    for (Class<? extends BaseTransaction> cl : ALL_SUB_TYPES) {
      BaseTransaction base;
      try {
        Constructor<? extends BaseTransaction> constructor = cl.getConstructor();
        base = constructor.newInstance();
      } catch (NoSuchMethodException e) {
        throw new InternalError("Class " + cl + " does not have a zero-argument constructor");
      } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
        throw new InternalError("Class " + cl + " could not be constructed", e);
      }
      TxType txType = base.getTxType();
      JavaType javaType = context.constructType(cl);
      decode.put(txType, javaType);
    }

    isInitialised = true;
  }


  @Override
  public JavaType typeFromId(DatabindContext context, String id) {
    initialise(context);
    TxType txType = TxType.get(id);
    return decode.get(txType);
  }

}
