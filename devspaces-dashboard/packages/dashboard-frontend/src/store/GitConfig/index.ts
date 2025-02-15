/*
 * Copyright (c) 2018-2023 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */

import common, { api, helpers } from '@eclipse-che/common';
import { AppThunk } from '..';
import {
  fetchGitConfig,
  patchGitConfig,
} from '../../services/dashboard-backend-client/gitConfigApi';
import { selectDefaultNamespace } from '../InfrastructureNamespaces/selectors';
import { AUTHORIZED } from '../sanityCheckMiddleware';
import { GitConfigUser, KnownAction, Type } from './types';
export * from './reducer';
export * from './types';

export type ActionCreators = {
  requestGitConfig: () => AppThunk<KnownAction, Promise<void>>;
  updateGitConfig: (gitconfig: GitConfigUser) => AppThunk<KnownAction, Promise<void>>;
};

export const actionCreators: ActionCreators = {
  requestGitConfig:
    (): AppThunk<KnownAction, Promise<void>> =>
    async (dispatch, getState): Promise<void> => {
      await dispatch({ type: Type.REQUEST_GITCONFIG, check: AUTHORIZED });

      const state = getState();
      const namespace = selectDefaultNamespace(state).name;
      try {
        const config = await fetchGitConfig(namespace);
        dispatch({
          type: Type.RECEIVE_GITCONFIG,
          config,
        });
      } catch (e) {
        if (common.helpers.errors.includesAxiosResponse(e) && e.response.status === 404) {
          dispatch({
            type: Type.RECEIVE_GITCONFIG,
            config: undefined,
          });
          return;
        }

        const errorMessage = helpers.errors.getMessage(e);
        dispatch({
          type: Type.RECEIVE_GITCONFIG_ERROR,
          error: errorMessage,
        });
        throw e;
      }
    },

  updateGitConfig:
    (changedGitConfig: GitConfigUser): AppThunk<KnownAction, Promise<void>> =>
    async (dispatch, getState): Promise<void> => {
      await dispatch({ type: Type.REQUEST_GITCONFIG, check: AUTHORIZED });

      const namespace = selectDefaultNamespace(getState()).name;
      const { gitConfig } = getState();
      const gitconfig = Object.assign(gitConfig.config || {}, {
        gitconfig: {
          user: changedGitConfig,
        },
      } as api.IGitConfig);
      try {
        const updated = await patchGitConfig(namespace, gitconfig);
        dispatch({
          type: Type.RECEIVE_GITCONFIG,
          config: updated,
        });
      } catch (e) {
        const errorMessage = helpers.errors.getMessage(e);
        dispatch({
          type: Type.RECEIVE_GITCONFIG_ERROR,
          error: errorMessage,
        });
        throw e;
      }
    },
};
